package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class DuplicateProductSlugException extends ConflictException {

    public DuplicateProductSlugException(String slug) {
        super("Product slug already exists: " + slug);
    }
}
