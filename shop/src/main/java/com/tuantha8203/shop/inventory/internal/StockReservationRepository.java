package com.tuantha8203.shop.inventory.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderRef(String orderRef);

    // SKIP LOCKED: if several app instances run the expiry job at once,
    // each one takes different rows instead of waiting for or double-processing the same ones
    @Query(value = """
                    SELECT * FROM inventory.stock_reservations
                    WHERE status = 'ACTIVE' AND expires_at < :now
                    ORDER BY expires_at
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """, nativeQuery = true)
    List<StockReservation> lockExpired(@Param("now") Instant now, @Param("limit") int limit);
}
