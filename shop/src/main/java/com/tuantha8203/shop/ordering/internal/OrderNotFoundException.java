package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(long orderId) {
        super("Order not found: " + orderId);
    }
}
