package tuantha8203.productservice.events;

import java.util.UUID;

public record ProductDeletedEvent(UUID productId) {}