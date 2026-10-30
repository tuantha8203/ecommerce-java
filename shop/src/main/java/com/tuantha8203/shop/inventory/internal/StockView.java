package com.tuantha8203.shop.inventory.internal;

public record StockView(long productId, int onHand, int reserved, int available) {

    static StockView from(StockItem item) {
        return new StockView(item.getProductId(), item.getOnHand(), item.getReserved(), item.available());
    }

    static StockView empty(long productId) {
        return new StockView(productId, 0, 0, 0);
    }
}
