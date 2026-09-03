package tuantha8203.searchservice.events;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreatedEvent(UUID productId, String sku, String name, String description, BigDecimal price) {}
