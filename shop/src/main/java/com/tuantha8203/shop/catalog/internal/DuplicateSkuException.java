package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class DuplicateSkuException extends ConflictException {

    public DuplicateSkuException(String sku) {
        super("SKU already exists: " + sku);
    }
}
