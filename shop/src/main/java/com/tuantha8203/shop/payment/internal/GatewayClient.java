package com.tuantha8203.shop.payment.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** The only class that talks HTTP to the payment gateway. */
@Component
@RequiredArgsConstructor
class GatewayClient {

    record ChargeResponse(String id, String reference, long amount, String status, String failureCode) {}

    record ChargeBody(String reference, long amount, String currency, String cardToken) {}

    private final RestClient paymentGatewayRestClient;

    /**
     * Retried on timeouts and 5xx only. Safe because of the Idempotency-Key: if the first attempt
     * did charge but its response was lost, the retry gets the same charge back instead of a new one.
     * A declined card is a normal answer, not an exception, so it is never retried.
     */
    @Retryable(
            includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2,
            delay = 200,
            multiplier = 2,
            jitter = 50)
    public ChargeResponse charge(String idempotencyKey, String reference, long amount, String paymentToken) {
        return paymentGatewayRestClient
                .post()
                .uri("/charges")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChargeBody(reference, amount, "VND", paymentToken))
                .retrieve()
                .body(ChargeResponse.class);
    }

    @Retryable(
            includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2,
            delay = 200)
    public List<ChargeResponse> findByReference(String reference) {
        return paymentGatewayRestClient
                .get()
                .uri("/charges?reference={reference}", reference)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ChargeResponse>>() {});
    }

    @Retryable(
            includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2,
            delay = 200)
    public ChargeResponse refund(String chargeId) {
        return paymentGatewayRestClient
                .post()
                .uri("/charges/{chargeId}/refund", chargeId)
                .retrieve()
                .body(ChargeResponse.class);
    }
}
