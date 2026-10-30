package com.tuantha8203.shop.ordering.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class IdempotencyKeys {

    enum State {
        CLAIMED, // this request owns the key and should do the work
        IN_PROGRESS, // another request with the same key is still running
        COMPLETED // the work was already done; orderId tells which order
    }

    record Claim(State state, Long orderId) {}

    private final JdbcClient jdbc;

    /** Own transaction, committed immediately, so concurrent requests see the claim right away. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Claim claim(long customerId, String key) {
        // ON CONFLICT DO NOTHING: a duplicate key returns 0 rows instead of throwing,
        // so the transaction is not aborted and we can still read the existing row
        int inserted =
                jdbc.sql("""
                        INSERT INTO ordering.idempotency_keys (customer_id, idempotency_key, status)
                        VALUES (:customerId, :key, 'IN_PROGRESS')
                        ON CONFLICT DO NOTHING
                        """).param("customerId", customerId).param("key", key).update();
        if (inserted == 1) {
            return new Claim(State.CLAIMED, null);
        }
        return jdbc.sql("""
                        SELECT status, order_id FROM ordering.idempotency_keys
                        WHERE customer_id = :customerId AND idempotency_key = :key
                        """)
                .param("customerId", customerId)
                .param("key", key)
                .query((rs, rowNum) ->
                        new Claim(State.valueOf(rs.getString("status")), (Long) rs.getObject("order_id")))
                .single();
    }

    /** Joins the caller's transaction: the key is completed if and only if the order is created. */
    @Transactional
    void complete(long customerId, String key, long orderId) {
        jdbc.sql("""
                        UPDATE ordering.idempotency_keys SET status = 'COMPLETED', order_id = :orderId
                        WHERE customer_id = :customerId AND idempotency_key = :key
                        """)
                .param("orderId", orderId)
                .param("customerId", customerId)
                .param("key", key)
                .update();
    }

    /** The attempt failed: free the key so the client can fix the problem and retry with it. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void release(long customerId, String key) {
        jdbc.sql("DELETE FROM ordering.idempotency_keys WHERE customer_id = :customerId AND idempotency_key = :key")
                .param("customerId", customerId)
                .param("key", key)
                .update();
    }
}
