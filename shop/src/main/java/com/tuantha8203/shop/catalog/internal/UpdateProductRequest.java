package com.tuantha8203.shop.catalog.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @PositiveOrZero long price) {}
