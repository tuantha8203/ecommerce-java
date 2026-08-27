package tuantha8203.orderservice.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        List<Item> items,
        BigDecimal totalAmount
) {
    public record Item(UUID productId, int quantity) {}
}