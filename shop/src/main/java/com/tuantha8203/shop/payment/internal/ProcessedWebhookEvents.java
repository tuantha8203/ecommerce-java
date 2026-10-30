package com.tuantha8203.shop.payment.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessedWebhookEvents {

    private final JdbcClient jdbc;

    /** True the first time an event id is seen, false for every duplicate delivery. */
    boolean markProcessed(String eventId) {
        return jdbc.sql("""
                        INSERT INTO payment.processed_webhook_events (event_id) VALUES (:eventId)
                        ON CONFLICT DO NOTHING
                        """).param("eventId", eventId).update() == 1;
    }
}
