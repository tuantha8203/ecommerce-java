package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class ProductNotForSaleException extends ConflictException {

    public ProductNotForSaleException(long productId) {
        super("Product " + productId + " is not for sale");
    }
}
