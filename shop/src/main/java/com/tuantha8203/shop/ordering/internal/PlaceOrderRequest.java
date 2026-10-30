package com.tuantha8203.shop.ordering.internal;

import jakarta.validation.constraints.Size;

/** Optional body. paymentToken lets you pick a test card, e.g. "tok_declined". */
public record PlaceOrderRequest(@Size(max = 100) String paymentToken) {}
