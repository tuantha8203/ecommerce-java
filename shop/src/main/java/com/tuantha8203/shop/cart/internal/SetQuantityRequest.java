package com.tuantha8203.shop.cart.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SetQuantityRequest(@Min(1) @Max(99) int quantity) {}
