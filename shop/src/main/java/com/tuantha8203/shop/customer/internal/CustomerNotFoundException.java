package com.tuantha8203.shop.customer.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(long id) {
        super("Customer not found: " + id);
    }
}
