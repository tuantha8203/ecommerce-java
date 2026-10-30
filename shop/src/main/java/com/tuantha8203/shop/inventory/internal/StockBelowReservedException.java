package com.tuantha8203.shop.inventory.internal;

import com.tuantha8203.shop.shared.ConflictException;

public class StockBelowReservedException extends ConflictException {

    public StockBelowReservedException(long productId, int onHand) {
        super("Cannot set stock of product " + productId + " to " + onHand + ": more units are currently reserved");
    }
}
