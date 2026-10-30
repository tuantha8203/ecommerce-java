package com.tuantha8203.shop.payment.internal;

record WebhookEvent(
        String eventId,
        String type,
        String chargeId,
        String reference,
        long amount,
        String currency,
        String status,
        String occurredAt) {}
