package com.tuantha8203.fakepayment;

public record ChaosSettings(
        long minLatencyMs,
        long maxLatencyMs,
        double failureRate,
        double declineRate,
        double lostResponseRate,
        long lostResponseDelayMs,
        double duplicateWebhookRate) {}