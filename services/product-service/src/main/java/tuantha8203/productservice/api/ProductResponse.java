package tuantha8203.productservice.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    String sku,
    BigDecimal price,
    Instant createdAt,
    Instant updatedAt
) {}