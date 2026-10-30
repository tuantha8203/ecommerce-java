package com.tuantha8203.shop.cart.internal;

import com.tuantha8203.shop.cart.CartLine;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CartRepository {

    private final JdbcClient jdbc;

    List<CartLine> findByCustomer(long customerId) {
        return jdbc.sql("""
                        SELECT product_id, quantity FROM cart.cart_items
                        WHERE customer_id = :customerId
                        ORDER BY added_at, product_id
                        """)
                .param("customerId", customerId)
                .query(CartLine.class) // columns product_id, quantity map to the record components
                .list();
    }

    /** Insert the line, or replace its quantity if the product is already in the cart. */
    void upsert(long customerId, long productId, int quantity) {
        jdbc.sql("""
                        INSERT INTO cart.cart_items (customer_id, product_id, quantity)
                        VALUES (:customerId, :productId, :quantity)
                        ON CONFLICT (customer_id, product_id)
                        DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = now()
                        """)
                .param("customerId", customerId)
                .param("productId", productId)
                .param("quantity", quantity)
                .update();
    }

    boolean contains(long customerId, long productId) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM cart.cart_items WHERE customer_id = :customerId AND product_id = :productId)
                        """)
                .param("customerId", customerId)
                .param("productId", productId)
                .query(Boolean.class)
                .single();
    }

    int countItems(long customerId) {
        return jdbc.sql("SELECT count(*) FROM cart.cart_items WHERE customer_id = :customerId")
                .param("customerId", customerId)
                .query(Integer.class)
                .single();
    }

    void delete(long customerId, long productId) {
        jdbc.sql("DELETE FROM cart.cart_items WHERE customer_id = :customerId AND product_id = :productId")
                .param("customerId", customerId)
                .param("productId", productId)
                .update();
    }

    void clear(long customerId) {
        jdbc.sql("DELETE FROM cart.cart_items WHERE customer_id = :customerId")
                .param("customerId", customerId)
                .update();
    }
}
