package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.cart.CartApi;
import com.tuantha8203.shop.cart.CartLine;
import com.tuantha8203.shop.catalog.CatalogApi;
import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.catalog.ProductSummary;
import com.tuantha8203.shop.customer.CustomerApi;
import com.tuantha8203.shop.inventory.InventoryApi;
import com.tuantha8203.shop.inventory.ReservationLine;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OrderPlacement {

    private final OrderRepository orders;
    private final IdempotencyKeys idempotencyKeys;
    private final CartApi cart;
    private final CatalogApi catalog;
    private final CustomerApi customers;
    private final InventoryApi inventory;

    /** Everything here commits together or not at all. */
    @Transactional
    OrderView place(long customerId, String idempotencyKey) {
        List<CartLine> cartLines = cart.lines(customerId);
        if (cartLines.isEmpty()) {
            throw new EmptyCartException();
        }

        ShippingDetails shipping = customers
                .findDefaultAddress(customerId)
                .map(ShippingDetails::copyOf)
                .orElseThrow(NoShippingAddressException::new);

        Map<Long, ProductSummary> products =
                catalog
                        .findProductsByIds(
                                cartLines.stream().map(CartLine::productId).toList())
                        .stream()
                        .collect(Collectors.toMap(ProductSummary::id, Function.identity()));

        Order order = Order.place(customerId, shipping);
        for (CartLine line : cartLines) {
            ProductSummary product = products.get(line.productId());
            if (product == null || product.status() != ProductStatus.ACTIVE) {
                throw new ProductUnavailableException(line.productId());
            }
            // the price is copied NOW: later catalog price changes do not affect this order
            order.addLine(product.id(), product.name(), product.price(), line.quantity());
        }
        orders.saveAndFlush(order); // INSERT now: the reference used below needs the generated id

        // Throws InsufficientStockException when stock is short: the whole transaction rolls back
        inventory.reserve(
                order.reference(),
                cartLines.stream()
                        .map(line -> new ReservationLine(line.productId(), line.quantity()))
                        .toList());

        cart.clear(customerId);
        idempotencyKeys.complete(customerId, idempotencyKey, order.getId());
        return OrderView.from(order);
    }
}
