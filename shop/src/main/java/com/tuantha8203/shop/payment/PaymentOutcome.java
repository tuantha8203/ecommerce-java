package com.tuantha8203.shop.payment;

public enum PaymentOutcome {
    SUCCEEDED,
    DECLINED,
    UNKNOWN // the gateway did not answer; a webhook or the reconciliation job will settle it
}
