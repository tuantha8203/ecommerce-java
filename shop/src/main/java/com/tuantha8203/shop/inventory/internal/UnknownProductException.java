package com.tuantha8203.shop.inventory.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class UnknownProductException extends NotFoundException {

    public UnknownProductException(long productId) {
        super("Product not found: " + productId);
    }
}
