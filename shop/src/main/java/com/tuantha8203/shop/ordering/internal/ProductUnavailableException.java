package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class ProductUnavailableException extends ConflictException {
    public ProductUnavailableException(long productId) {
        super("Product " + productId + " is no longer for sale");
    }
}
