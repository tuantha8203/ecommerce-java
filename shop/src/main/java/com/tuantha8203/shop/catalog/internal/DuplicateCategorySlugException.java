package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class DuplicateCategorySlugException extends ConflictException {

    public DuplicateCategorySlugException(String slug) {
        super("Category slug already exists: " + slug);
    }
}
