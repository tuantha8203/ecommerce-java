package com.tuantha8203.shop.ordering;

/** Published inside the transaction that marks an order as paid. */
public record OrderPaid(long orderId, long customerId, long total) {}
