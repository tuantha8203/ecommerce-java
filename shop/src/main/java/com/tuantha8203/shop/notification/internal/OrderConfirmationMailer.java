package com.tuantha8203.shop.notification.internal;

import com.tuantha8203.shop.customer.CustomerApi;
import com.tuantha8203.shop.customer.CustomerContact;
import com.tuantha8203.shop.ordering.OrderPaid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmationMailer {

    private final CustomerApi customers;
    private final JavaMailSender mailSender;

    @Value("${ecommerce-java.notification.from:no-reply@ecommerce.local}")
    private String from;

    /**
     * AFTER_COMMIT: never confirm an order whose transaction rolled back.
     * Async on our own pool: the payment flow does not wait for the mail server.
     */
    @Async(NotificationConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaid event) {
        customers
                .findContact(event.customerId())
                .ifPresentOrElse(
                        contact -> send(contact, event),
                        () -> log.warn("Order {} paid by unknown customer {}", event.orderId(), event.customerId()));
    }

    private void send(CustomerContact contact, OrderPaid event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(contact.email());
        message.setSubject("Xác nhận đơn hàng #" + event.orderId());
        message.setText("""
                Chào %s,

                Đơn hàng #%d của bạn đã được thanh toán thành công.
                Tổng tiền: %s ₫

                Cảm ơn bạn đã mua sắm!
                """.formatted(
                        contact.fullName(),
                        event.orderId(),
                        String.format("%,d", event.total()).replace(',', '.')));
        try {
            mailSender.send(message);
            log.info("Confirmation for order {} sent to {}", event.orderId(), contact.email());
        } catch (MailException ex) {
            // The order is paid either way: a failed email must never undo or block that
            log.error("Could not send confirmation for order {}", event.orderId(), ex);
        }
    }
}
