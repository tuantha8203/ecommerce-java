package com.tuantha8203.shop.cart.internal;

import java.util.List;

public record CartView(List<CartLineView> lines, int itemCount, long total, boolean checkoutReady) {

    static final CartView EMPTY = new CartView(List.of(), 0, 0, false);

    static CartView of(List<CartLineView> lines) {
        int itemCount = lines.stream().mapToInt(CartLineView::quantity).sum();
        long total = lines.stream()
                .filter(CartLineView::purchasable)
                .mapToLong(CartLineView::lineTotal)
                .sum();
        boolean checkoutReady = !lines.isEmpty() && lines.stream().allMatch(CartLineView::purchasable);
        return new CartView(lines, itemCount, total, checkoutReady);
    }
}
