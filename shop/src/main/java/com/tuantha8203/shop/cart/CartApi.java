package com.tuantha8203.shop.cart;

import java.util.List;

public interface CartApi {

    /** What the customer wants to buy, in the order items were added. */
    List<CartLine> lines(long customerId);

    /** Empties the cart, e.g. once an order has been placed. */
    void clear(long customerId);
}
