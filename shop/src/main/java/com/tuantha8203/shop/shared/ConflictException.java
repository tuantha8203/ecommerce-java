package com.tuantha8203.shop.shared;

/** Base class for errors caused by the current state of data. Mapped to HTTP 409. */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
