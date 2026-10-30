package com.tuantha8203.shop.shared;

/** Base class for "resource does not exist" errors. Mapped to HTTP 404. */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
