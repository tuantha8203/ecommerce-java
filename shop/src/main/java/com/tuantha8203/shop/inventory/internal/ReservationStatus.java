package com.tuantha8203.shop.inventory.internal;

enum ReservationStatus {
    ACTIVE, // held, expires if the order is not paid in time
    CONFIRMED, // held for a paid order, never expires
    COMMITTED, // shipped: units left the warehouse
    RELEASED // back on sale
}
