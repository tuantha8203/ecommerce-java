package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.cart.CartApi;
import com.tuantha8203.shop.cart.CartLine;
import com.tuantha8203.shop.catalog.CatalogApi;
import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.catalog.ProductSummary;
import com.tuantha8203.shop.inventory.InventoryApi;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService implements CartApi {

    static final int MAX_DISTINCT_ITEMS = 50;

    private final CartRepository cartItems;
    private final CatalogApi catalog;
    private final InventoryApi inventory;

    public CartView getCart(long customerId) {
        List<CartLine> lines = cartItems.findByCustomer(customerId);
        if (lines.isEmpty()) {
            return CartView.EMPTY;
        }

        List<Long> productIds = lines.stream().map(CartLine::productId).toList();
        // one call to each module for the whole cart, never one call per line
        Map<Long, ProductSummary> products = catalog.findProductsByIds(productIds).stream()
                .collect(Collectors.toMap(ProductSummary::id, Function.identity()));
        Map<Long, Integer> stock = inventory.availability(productIds);

        List<CartLineView> views = lines.stream()
                .map(line ->
                        CartLineView.of(line, products.get(line.productId()), stock.getOrDefault(line.productId(), 0)))
                .toList();
        return CartView.of(views);
    }

    @Transactional
    public CartView setQuantity(long customerId, long productId, int quantity) {
        ProductSummary product = catalog.findProductsByIds(List.of(productId)).stream()
                .findFirst()
                .orElseThrow(() -> new CartProductNotFoundException(productId));
        if (product.status() != ProductStatus.ACTIVE) {
            throw new ProductNotForSaleException(productId);
        }
        if (!cartItems.contains(customerId, productId) && cartItems.countItems(customerId) >= MAX_DISTINCT_ITEMS) {
            throw new CartFullException(MAX_DISTINCT_ITEMS);
        }
        cartItems.upsert(customerId, productId, quantity);
        return getCart(customerId);
    }

    @Transactional
    public CartView removeItem(long customerId, long productId) {
        cartItems.delete(customerId, productId); // removing something that is not there is not an error
        return getCart(customerId);
    }

    @Override
    public List<CartLine> lines(long customerId) {
        return cartItems.findByCustomer(customerId);
    }

    @Override
    @Transactional
    public void clear(long customerId) {
        cartItems.clear(customerId);
    }
}
