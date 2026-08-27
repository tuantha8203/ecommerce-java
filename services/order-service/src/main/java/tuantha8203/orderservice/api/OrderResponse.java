package tuantha8203.orderservice.api;

import tuantha8203.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {}
}