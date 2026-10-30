package com.tuantha8203.shop.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockItemRepository extends JpaRepository<StockItem, Long> {

    // Check and update in ONE statement: the database serializes concurrent calls on the same row
    // and re-evaluates the WHERE clause on the latest value. Returns 0 when there is not enough stock.
    @Modifying(flushAutomatically = true)
    @Query("""
            update StockItem s set s.reserved = s.reserved + :qty
            where s.productId = :productId and s.onHand - s.reserved >= :qty
            """)
    int tryReserve(@Param("productId") long productId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("""
            update StockItem s set s.reserved = s.reserved - :qty
            where s.productId = :productId and s.reserved >= :qty
            """)
    int release(@Param("productId") long productId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("""
            update StockItem s set s.onHand = s.onHand - :qty, s.reserved = s.reserved - :qty
            where s.productId = :productId and s.reserved >= :qty
            """)
    int commit(@Param("productId") long productId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query(value = """
                    INSERT INTO inventory.stock_items (product_id, on_hand, reserved)
                    VALUES (:productId, :onHand, 0)
                    ON CONFLICT (product_id) DO UPDATE SET on_hand = EXCLUDED.on_hand
                    """, nativeQuery = true)
    int upsertOnHand(@Param("productId") long productId, @Param("onHand") int onHand);
}
