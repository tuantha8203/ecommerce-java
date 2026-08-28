package tuantha8203.productservice.events;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdatedEvent(UUID productId, String sku, String name, String description, BigDecimal price) {}