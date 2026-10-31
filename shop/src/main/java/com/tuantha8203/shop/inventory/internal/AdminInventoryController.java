package com.tuantha8203.shop.inventory.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
class AdminInventoryController {

    private final InventoryService inventory;

    @GetMapping("/{productId}")
    StockView get(@PathVariable long productId) {
        return inventory.getStock(productId);
    }

    @PutMapping("/{productId}")
    StockView set(@PathVariable long productId, @Valid @RequestBody SetStockRequest request) {
        return inventory.setOnHand(productId, request.onHand());
    }
}
