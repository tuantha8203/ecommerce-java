package com.tuantha8203.fakepayment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"fake-payment.chaos.min-latency-ms=0", "fake-payment.chaos.max-latency-ms=0"})
@AutoConfigureMockMvc
class FakePaymentApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Chaos chaos;

    private static final ChaosSettings CALM = new ChaosSettings(0, 0, 0, 0, 0, 0, 0);

    @AfterEach
    void calmDown() {
        chaos.update(CALM);
    }

    private ResultActions charge(String key, String reference, long amount, String cardToken) throws Exception {
        return mvc.perform(post("/charges")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reference": "%s", "amount": %d, "currency": "VND", "cardToken": "%s"}
                        """.formatted(reference, amount, cardToken)));
    }

    @Test
    void sameKeyReturnsTheSameChargeInsteadOfChargingTwice() throws Exception {
        String key = UUID.randomUUID().toString();
        String reference = "order-" + UUID.randomUUID();

        String first = charge(key, reference, 150_000, "tok_visa")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = charge(key, reference, 150_000, "tok_visa")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        mvc.perform(get("/charges").param("reference", reference)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void reusingAKeyForADifferentPaymentIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        charge(key, "order-a", 100_000, "tok_visa").andExpect(status().isCreated());

        charge(key, "order-a", 999_000, "tok_visa").andExpect(status().is(422));
    }

    @Test
    void declinedTestCardIsDeclined() throws Exception {
        charge(UUID.randomUUID().toString(), "order-d", 100_000, "tok_declined")
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.failureCode").value("card_declined"));
    }

    @Test
    void failureBeforeProcessingLeavesNoCharge() throws Exception {
        chaos.update(new ChaosSettings(0, 0, 1.0, 0, 0, 0, 0)); // every call fails
        String reference = "order-" + UUID.randomUUID();

        charge(UUID.randomUUID().toString(), reference, 100_000, "tok_visa")
                .andExpect(status().isInternalServerError());

        mvc.perform(get("/charges").param("reference", reference)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingIdempotencyKeyIsRejected() throws Exception {
        mvc.perform(post("/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference": "order-x", "amount": 100, "currency": "VND"}
                                """))
                .andExpect(status().isBadRequest());
    }
}