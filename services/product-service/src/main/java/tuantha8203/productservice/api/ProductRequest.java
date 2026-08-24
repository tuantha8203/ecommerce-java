package tuantha8203.productservice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank @Size(min = 2, max = 255) String name,
    @Size(max = 255) String description,
    @NotBlank @Size(max = 64) String sku,
    @Positive BigDecimal price
) {}