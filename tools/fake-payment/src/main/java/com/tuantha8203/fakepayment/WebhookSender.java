package com.tuantha8203.fakepayment;

import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Tells the shop about the outcome of a charge, asynchronously, the way real gateways do. */
@Slf4j
@Component
public class WebhookSender {

    private final FakePaymentProperties properties;
    private final Chaos chaos;
    private final RestClient client;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WebhookSender(FakePaymentProperties properties, Chaos chaos) {
        this.properties = properties;
        this.chaos = chaos;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    public void send(Charge charge) {
        String eventId = "evt_" + UUID.randomUUID();
        String type = "charge." + charge.status().name().toLowerCase();
        String body = """
                {"eventId":"%s","type":"%s","chargeId":"%s","reference":"%s","amount":%d,"currency":"%s","status":"%s","occurredAt":"%s"}"""
                .formatted(
                        eventId,
                        type,
                        charge.id(),
                        charge.reference(),
                        charge.amount(),
                        charge.currency(),
                        charge.status(),
                        Instant.now());

        executor.submit(() -> {
            chaos.sleep(Duration.ofMillis(500)); // the webhook arrives a little after the API response
            deliver(eventId, body);
            if (chaos.happens(chaos.settings().duplicateWebhookRate())) {
                deliver(eventId, body); // same event twice: the receiver must deduplicate by eventId
            }
        });
    }

    private void deliver(String eventId, String body) {
        try {
            client.post()
                    .uri(properties.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Fake-Payment-Event-Id", eventId)
                    .header("X-Fake-Payment-Signature", sign(body))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Webhook {} delivered", eventId);
        } catch (RestClientException ex) {
            log.warn("Webhook {} delivery failed: {}", eventId, ex.getMessage());
        }
    }

    /** HMAC-SHA256 of the exact body bytes, hex-encoded: proves the webhook came from us and was not altered. */
    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot sign webhook", ex);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}