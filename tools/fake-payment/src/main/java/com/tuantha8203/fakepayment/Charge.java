package com.tuantha8203.fakepayment;

import java.time.Instant;

public record Charge(
        String id,
        String reference,
        long amount,
        String currency,
        ChargeStatus status,
        String failureCode,
        Instant createdAt) {

    Charge withStatus(ChargeStatus newStatus) {
        return new Charge(id, reference, amount, currency, newStatus, failureCode, createdAt);
    }
}