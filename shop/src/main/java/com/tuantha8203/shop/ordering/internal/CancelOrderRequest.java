package com.tuantha8203.shop.ordering.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(@NotBlank @Size(max = 255) String reason) {}
