package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException(long id) {
        super("Category not found: " + id);
    }
}
