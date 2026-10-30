package com.tuantha8203.shop.payment;

public interface PaymentApi {

    /**
     * Charges the order at most once, however often it is called for the same orderRef.
     * Never throws because of gateway problems: returns UNKNOWN instead.
     * The result is also published as PaymentSucceeded or PaymentFailed.
     */
    PaymentOutcome charge(String orderRef, long amount, String paymentToken);

    /** Queues a refund; a background job performs it. Does nothing if the order was never paid. */
    void requestRefund(String orderRef);
}
