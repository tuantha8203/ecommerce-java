package com.tuantha8203.shop.payment.internal;

import com.tuantha8203.shop.payment.PaymentOutcome;

enum PaymentStatus {
    PENDING, // created, gateway call in progress
    UNKNOWN, // gateway call failed without an answer
    SUCCEEDED,
    DECLINED,
    REFUND_REQUESTED,
    REFUNDED;

    PaymentOutcome toOutcome() {
        return switch (this) {
            case SUCCEEDED, REFUND_REQUESTED, REFUNDED -> PaymentOutcome.SUCCEEDED;
            case DECLINED -> PaymentOutcome.DECLINED;
            case PENDING, UNKNOWN -> PaymentOutcome.UNKNOWN;
        };
    }
}
