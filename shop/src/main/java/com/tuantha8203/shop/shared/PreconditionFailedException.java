package com.tuantha8203.shop.shared;

/** The client's view of the resource is outdated (If-Match does not match). Mapped to HTTP 412. */
public class PreconditionFailedException extends RuntimeException {

    public PreconditionFailedException(String message) {
        super(message);
    }
}
