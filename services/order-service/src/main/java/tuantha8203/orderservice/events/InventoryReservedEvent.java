package tuantha8203.orderservice.events;

import java.util.UUID;

public record InventoryReservedEvent(UUID orderId) {}