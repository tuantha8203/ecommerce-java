package com.tuantha8203.shop.inventory;

public record ReservationLine(long productId, int quantity) {

    public ReservationLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
