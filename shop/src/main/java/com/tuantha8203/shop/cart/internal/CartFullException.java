package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class CartFullException extends ConflictException {

    public CartFullException(int maxItems) {
        super("A cart can hold at most " + maxItems + " different products");
    }
}
