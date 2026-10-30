package com.tuantha8203.shop.payment.internal;

import com.tuantha8203.shop.payment.PaymentApi;
import com.tuantha8203.shop.payment.PaymentOutcome;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentApi {

    private final PaymentRecorder recorder;
    private final GatewayClient gateway;
    private final PaymentRepository payments;
    private final ProcessedWebhookEvents processedEvents;
    private final PaymentProperties properties;

    /** Deliberately NOT @Transactional: no database connection is held while we wait for the gateway. */
    @Override
    public PaymentOutcome charge(String orderRef, long amount, String paymentToken) {
        PaymentAttempt attempt = recorder.start(orderRef, amount, paymentToken);
        if (attempt.status() != PaymentStatus.PENDING && attempt.status() != PaymentStatus.UNKNOWN) {
            return attempt.status().toOutcome(); // already settled earlier
        }

        GatewayClient.ChargeResponse response;
        try {
            response = gateway.charge(attempt.idempotencyKey(), orderRef, amount, paymentToken);
        } catch (RestClientException ex) {
            // Retries exhausted. We do NOT know whether money moved: never treat this as a failure
            log.warn("Payment for {} has an unknown outcome: {}", orderRef, ex.getMessage());
            return recorder.recordUnknown(orderRef);
        }
        return apply(orderRef, response.status(), response.id(), response.failureCode());
    }

    @Override
    public void requestRefund(String orderRef) {
        recorder.requestRefund(orderRef);
    }

    /** One transaction: remembering the event id and applying it succeed or fail together. */
    @Transactional
    public void handleWebhook(WebhookEvent event) {
        if (!processedEvents.markProcessed(event.eventId())) {
            log.info("Duplicate webhook {} ignored", event.eventId());
            return;
        }
        if (payments.findByOrderRef(event.reference()).isEmpty()) {
            log.warn("Webhook {} refers to unknown payment {}", event.eventId(), event.reference());
            return;
        }
        apply(event.reference(), event.status(), event.chargeId(), null);
    }

    /** Asks the gateway about payments stuck in PENDING or UNKNOWN. */
    public int reconcile(Instant now) {
        List<Payment> stuck = payments.findTop50ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                List.of(PaymentStatus.PENDING, PaymentStatus.UNKNOWN), now.minus(properties.reconcileAfter()));

        for (Payment payment : stuck) {
            List<GatewayClient.ChargeResponse> charges;
            try {
                charges = gateway.findByReference(payment.getOrderRef());
            } catch (RestClientException ex) {
                log.warn("Reconciliation of {} failed: {}", payment.getOrderRef(), ex.getMessage());
                continue; // try again on the next run
            }
            var succeeded =
                    charges.stream().filter(c -> "SUCCEEDED".equals(c.status())).findFirst();
            var declined =
                    charges.stream().filter(c -> "DECLINED".equals(c.status())).findFirst();

            if (succeeded.isPresent()) {
                recorder.recordSuccess(payment.getOrderRef(), succeeded.get().id());
            } else if (declined.isPresent()) {
                recorder.recordDecline(payment.getOrderRef(), declined.get().failureCode());
            } else if (payment.getCreatedAt().isBefore(now.minus(properties.giveUpAfter()))) {
                recorder.recordDecline(payment.getOrderRef(), "No charge found at the gateway");
            }
        }
        return stuck.size();
    }

    /** Performs queued refunds; each HTTP call happens outside any transaction. */
    public int processRefunds() {
        List<Payment> queued = payments.findTop50ByStatusOrderByUpdatedAtAsc(PaymentStatus.REFUND_REQUESTED);
        for (Payment payment : queued) {
            try {
                gateway.refund(payment.getGatewayChargeId());
                recorder.markRefunded(payment.getOrderRef());
            } catch (RestClientException ex) {
                log.warn("Refund of {} failed, will retry: {}", payment.getOrderRef(), ex.getMessage());
            }
        }
        return queued.size();
    }

    private PaymentOutcome apply(String orderRef, String gatewayStatus, String chargeId, String failureCode) {
        return switch (gatewayStatus) {
            case "SUCCEEDED" -> recorder.recordSuccess(orderRef, chargeId);
            case "DECLINED" ->
                recorder.recordDecline(orderRef, failureCode == null ? "Declined by gateway" : failureCode);
            default -> recorder.recordUnknown(orderRef);
        };
    }
}
