package com.tuantha8203.shop.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.customer.internal.CustomerService;
import com.tuantha8203.shop.customer.internal.RegisterCustomerRequest;
import com.tuantha8203.shop.ordering.OrderPaid;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderConfirmationMailerIntegrationTest {

    @MockitoBean
    JavaMailSenderImpl mailSender; // the concrete class: satisfies every injection point of the real bean

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    PlatformTransactionManager transactionManager; // always present when JPA is on the classpath

    @Autowired
    CustomerService customerService;

    private TransactionTemplate transactions() {
        return new TransactionTemplate(transactionManager);
    }

    private long customer(String email) {
        return customerService
                .register(new RegisterCustomerRequest(email, "An Nguyen", null))
                .id();
    }

    @Test
    void aCommittedPaymentSendsTheConfirmation() {
        String email = "mail-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        long customerId = customer(email);

        transactions().executeWithoutResult(status -> events.publishEvent(new OrderPaid(42, customerId, 1_250_000)));

        // the mail is sent on another thread: wait for it instead of guessing a sleep duration
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mailSender).send(sent.capture()));

        assertThat(sent.getValue().getTo()).containsExactly(email);
        assertThat(sent.getValue().getSubject()).contains("#42");
        assertThat(sent.getValue().getText()).contains("1.250.000");
    }

    @Test
    void aRolledBackPaymentSendsNothing() {
        long customerId = customer("rollback-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local");

        transactions().executeWithoutResult(status -> {
            events.publishEvent(new OrderPaid(43, customerId, 100_000));
            status.setRollbackOnly(); // the payment transaction fails after the event was published
        });

        // keep checking for a full second that nothing was ever sent
        await().during(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(mailSender, never()).send(any(SimpleMailMessage.class)));
    }
}
