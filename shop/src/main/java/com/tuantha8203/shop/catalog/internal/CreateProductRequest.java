package com.tuantha8203.shop.catalog.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotNull Long categoryId,

        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Z0-9-]+")
        String sku,

        @NotBlank @Size(max = 255) String name,

        @NotBlank @Size(max = 255) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*")
        String slug,

        @Size(max = 5000) String description,
        @PositiveOrZero long price) {}
