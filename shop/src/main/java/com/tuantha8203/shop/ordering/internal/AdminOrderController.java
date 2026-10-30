package com.tuantha8203.shop.ordering.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
class AdminOrderController {

    private final OrderService orders;

    @PostMapping("/{orderId}/shipment")
    OrderView ship(@PathVariable long orderId) {
        return orders.ship(orderId);
    }

    @PostMapping("/{orderId}/delivery")
    OrderView deliver(@PathVariable long orderId) {
        return orders.deliver(orderId);
    }

    @PostMapping("/{orderId}/cancellation")
    OrderView cancel(@PathVariable long orderId, @Valid @RequestBody CancelOrderRequest request) {
        return orders.cancelByAdmin(orderId, request.reason());
    }
}
