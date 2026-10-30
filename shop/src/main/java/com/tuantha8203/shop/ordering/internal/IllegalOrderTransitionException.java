package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class IllegalOrderTransitionException extends ConflictException {
    public IllegalOrderTransitionException(Long orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot go from " + from + " to " + to);
    }
}
