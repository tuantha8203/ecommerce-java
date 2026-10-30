package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class NoShippingAddressException extends ConflictException {
    public NoShippingAddressException() {
        super("Add a shipping address before placing an order");
    }
}
