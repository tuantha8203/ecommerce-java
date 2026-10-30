package com.tuantha8203.shop.customer.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
