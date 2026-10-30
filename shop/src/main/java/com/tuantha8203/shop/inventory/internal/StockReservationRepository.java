package com.tuantha8203.shop.inventory.internal;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // FOR UPDATE: the expiry job uses SKIP LOCKED, so it can never release a reservation
    // while confirm(), release() or commit() is working on it, and vice versa
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReservation r where r.orderRef = :orderRef")
    List<StockReservation> lockByOrderRef(@Param("orderRef") String orderRef);
}
