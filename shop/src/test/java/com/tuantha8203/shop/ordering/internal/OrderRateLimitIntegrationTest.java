package com.tuantha8203.shop.ordering.internal;

import static com.tuantha8203.shop.TestAuth.customer;
import static com.tuantha8203.shop.TestAuth.newSubject;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuantha8203.shop.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderRateLimitIntegrationTest {

    @Autowired
    MockMvc mvc;

    private ResultActions placeOrder(String subject) throws Exception {
        return mvc.perform(post("/api/v1/orders")
                .with(customer(subject))
                .header("Idempotency-Key", UUID.randomUUID().toString()));
    }

    @Test
    void theEleventhOrderAttemptInAMinuteIsRefused() throws Exception {
        String spammer = newSubject();

        // the cart is empty, so each attempt fails with 409, but it still counts towards the limit
        for (int i = 0; i < 10; i++) {
            placeOrder(spammer).andExpect(status().isConflict());
        }

        placeOrder(spammer).andExpect(status().isTooManyRequests()).andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void oneCustomersLimitDoesNotAffectAnother() throws Exception {
        String spammer = newSubject();
        for (int i = 0; i < 11; i++) {
            placeOrder(spammer);
        }

        placeOrder(newSubject()).andExpect(status().isConflict()); // 409 empty cart, not 429
    }
}
