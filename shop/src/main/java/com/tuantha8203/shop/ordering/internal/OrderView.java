package com.tuantha8203.shop.ordering.internal;

import java.time.Instant;
import java.util.List;

public record OrderView(
        Long id,
        OrderStatus status,
        long total,
        Shipping shipping,
        List<Line> lines,
        String cancelReason,
        Instant createdAt) {

    public record Shipping(String receiverName, String phone, String street, String ward, String province) {}

    public record Line(long productId, String productName, long unitPrice, int quantity, long lineTotal) {}

    /** Needs the lines loaded: call inside a transaction, or after fetching them with the order. */
    static OrderView from(Order order) {
        ShippingDetails s = order.getShipping();
        return new OrderView(
                order.getId(),
                order.getStatus(),
                order.getTotal(),
                new Shipping(s.getReceiverName(), s.getPhone(), s.getStreet(), s.getWard(), s.getProvince()),
                order.getLines().stream()
                        .map(line -> new Line(
                                line.getProductId(),
                                line.getProductName(),
                                line.getUnitPrice(),
                                line.getQuantity(),
                                line.lineTotal()))
                        .toList(),
                order.getCancelReason(),
                order.getCreatedAt());
    }
}
