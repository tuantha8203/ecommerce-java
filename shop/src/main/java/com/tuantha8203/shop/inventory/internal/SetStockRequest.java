package com.tuantha8203.shop.inventory.internal;

import jakarta.validation.constraints.PositiveOrZero;

public record SetStockRequest(@PositiveOrZero int onHand) {}
