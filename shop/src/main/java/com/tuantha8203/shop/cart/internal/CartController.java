package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.customer.CurrentCustomerId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
class CartController {

    private final CartService cart;

    @GetMapping
    CartView get(@CurrentCustomerId long customerId) {
        return cart.getCart(customerId);
    }

    @PutMapping("/items/{productId}")
    CartView setQuantity(
            @CurrentCustomerId long customerId,
            @PathVariable long productId,
            @Valid @RequestBody SetQuantityRequest request) {
        return cart.setQuantity(customerId, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    CartView remove(@CurrentCustomerId long customerId, @PathVariable long productId) {
        return cart.removeItem(customerId, productId);
    }
}
