package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class CartProductNotFoundException extends NotFoundException {

    public CartProductNotFoundException(long productId) {
        super("Product not found: " + productId);
    }
}
