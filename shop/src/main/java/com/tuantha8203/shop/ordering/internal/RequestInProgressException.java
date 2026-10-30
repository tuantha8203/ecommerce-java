package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class RequestInProgressException extends ConflictException {
    public RequestInProgressException() {
        super("A request with this Idempotency-Key is still being processed");
    }
}
