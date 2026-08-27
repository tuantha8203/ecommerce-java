package tuantha8203.orderservice.events;

import java.util.UUID;

public record InventoryFailedEvent(UUID orderId, String reason) {}