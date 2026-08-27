package tuantha8203.inventoryservice.events;

import java.util.UUID;

public record InventoryFailedEvent(UUID orderId, String reason) {}