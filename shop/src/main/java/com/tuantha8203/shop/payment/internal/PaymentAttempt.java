package com.tuantha8203.shop.payment.internal;

/** Plain snapshot handed out of a transaction, so no detached entity travels around. */
record PaymentAttempt(String orderRef, String idempotencyKey, long amount, String paymentToken, PaymentStatus status) {

    static PaymentAttempt from(Payment payment) {
        return new PaymentAttempt(
                payment.getOrderRef(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getPaymentToken(),
                payment.getStatus());
    }
}
