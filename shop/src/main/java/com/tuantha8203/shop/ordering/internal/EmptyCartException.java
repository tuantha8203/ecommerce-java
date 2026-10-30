package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class EmptyCartException extends ConflictException {
    public EmptyCartException() {
        super("The cart is empty");
    }
}
