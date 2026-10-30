package com.tuantha8203.shop.ordering.internal;

import java.time.Instant;

public record OrderSummary(Long id, OrderStatus status, long total, Instant createdAt) {

    static OrderSummary from(Order order) {
        return new OrderSummary(order.getId(), order.getStatus(), order.getTotal(), order.getCreatedAt());
    }
}
