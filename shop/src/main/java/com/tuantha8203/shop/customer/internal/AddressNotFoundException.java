package com.tuantha8203.shop.customer.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class AddressNotFoundException extends NotFoundException {

    public AddressNotFoundException(long id) {
        super("Address not found: " + id);
    }
}
