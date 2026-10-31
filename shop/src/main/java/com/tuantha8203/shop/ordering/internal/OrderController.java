package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.customer.CurrentCustomerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orders;

    @PostMapping
    ResponseEntity<OrderView> place(
            @CurrentCustomerId long customerId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody(required = false) PlaceOrderRequest request,
            UriComponentsBuilder uriBuilder) {
        String paymentToken = request == null ? null : request.paymentToken();
        OrderService.PlacementResult result = orders.placeOrder(customerId, idempotencyKey, paymentToken);
        URI location = uriBuilder
                .path("/api/v1/orders/{id}")
                .buildAndExpand(result.order().id())
                .toUri();
        // 201 for a new order, 200 when the same key replays an earlier one
        return result.created()
                ? ResponseEntity.created(location).body(result.order())
                : ResponseEntity.ok().location(location).body(result.order());
    }

    @GetMapping
    List<OrderSummary> list(@CurrentCustomerId long customerId) {
        return orders.listOrders(customerId);
    }

    @GetMapping("/{orderId}")
    OrderView get(@CurrentCustomerId long customerId, @PathVariable long orderId) {
        return orders.getOrder(customerId, orderId);
    }

    @PostMapping("/{orderId}/cancellation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@CurrentCustomerId long customerId, @PathVariable long orderId) {
        orders.cancelByCustomer(customerId, orderId);
    }
}
