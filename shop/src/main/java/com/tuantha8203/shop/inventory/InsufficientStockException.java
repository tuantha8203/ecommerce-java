package com.tuantha8203.shop.inventory;

import com.tuantha8203.shop.shared.ConflictException;

public class InsufficientStockException extends ConflictException {

    private final long productId;

    public InsufficientStockException(long productId, int requested) {
        super("Not enough stock for product " + productId + " (requested " + requested + ")");
        this.productId = productId;
    }

    public long productId() {
        return productId;
    }
}
