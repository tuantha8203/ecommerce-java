package com.tuantha8203.shop.shared;

/** The request is syntactically fine but carries a value we cannot use. Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
