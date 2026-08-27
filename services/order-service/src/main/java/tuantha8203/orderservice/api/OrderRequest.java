package tuantha8203.orderservice.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderRequest(@NotEmpty @Valid List<Item> items) {
    public record Item(UUID productId, @Positive int quantity, @Positive BigDecimal unitPrice) {}
}