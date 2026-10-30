package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(String key) {
        super("Product not found: " + key);
    }
}
