package com.tuantha8203.shop.inventory;

import java.time.Instant;

public record Reservation(String orderRef, Instant expiresAt) {}
