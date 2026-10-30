package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.cart.CartLine;
import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.catalog.ProductSummary;

public record CartLineView(
        long productId,
        String name,
        String slug,
        long unitPrice,
        int quantity,
        long lineTotal,
        int availableStock,
        boolean purchasable) {

    /** product is null when it no longer exists in the catalog. */
    static CartLineView of(CartLine line, ProductSummary product, int availableStock) {
        if (product == null) {
            return new CartLineView(line.productId(), null, null, 0, line.quantity(), 0, 0, false);
        }
        boolean purchasable = product.status() == ProductStatus.ACTIVE && availableStock >= line.quantity();
        return new CartLineView(
                product.id(),
                product.name(),
                product.slug(),
                product.price(),
                line.quantity(),
                product.price() * line.quantity(),
                availableStock,
                purchasable);
    }
}
