package com.tuantha8203.shop.payment.internal;

import com.tuantha8203.shop.payment.PaymentFailed;
import com.tuantha8203.shop.payment.PaymentOutcome;
import com.tuantha8203.shop.payment.PaymentSucceeded;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Every database step of a payment, each one a short transaction. No HTTP calls in here. */
@Component
@RequiredArgsConstructor
class PaymentRecorder {

    private final PaymentRepository payments;
    private final ApplicationEventPublisher events;

    @Transactional
    PaymentAttempt start(String orderRef, long amount, String paymentToken) {
        Payment payment = payments.findByOrderRef(orderRef)
                .orElseGet(() -> payments.save(Payment.start(orderRef, amount, paymentToken)));
        return PaymentAttempt.from(payment);
    }

    /**
     * Listeners of PaymentSucceeded run synchronously inside THIS transaction:
     * the payment and the order change state together, or not at all.
     */
    @Transactional
    PaymentOutcome recordSuccess(String orderRef, String chargeId) {
        Payment payment = lock(orderRef);
        if (payment.succeed(chargeId)) {
            events.publishEvent(new PaymentSucceeded(orderRef));
        }
        return payment.getStatus().toOutcome();
    }

    @Transactional
    PaymentOutcome recordDecline(String orderRef, String reason) {
        Payment payment = lock(orderRef);
        if (payment.decline(reason)) {
            events.publishEvent(new PaymentFailed(orderRef, reason));
        }
        return payment.getStatus().toOutcome();
    }

    @Transactional
    PaymentOutcome recordUnknown(String orderRef) {
        Payment payment = lock(orderRef);
        payment.markUnknown();
        return payment.getStatus().toOutcome();
    }

    @Transactional
    void requestRefund(String orderRef) {
        payments.lockByOrderRef(orderRef).ifPresent(Payment::requestRefund);
    }

    @Transactional
    void markRefunded(String orderRef) {
        lock(orderRef).markRefunded();
    }

    private Payment lock(String orderRef) {
        return payments.lockByOrderRef(orderRef)
                .orElseThrow(() -> new IllegalStateException("No payment for " + orderRef));
    }
}
