package com.tuantha8203.shop.inventory.internal;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "inventory", name = "stock_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderRef;

    @Column(nullable = false)
    private long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    StockReservation(String orderRef, long productId, int quantity, Instant expiresAt) {
        this.orderRef = orderRef;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.ACTIVE;
    }

    boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    void release() {
        this.status = ReservationStatus.RELEASED;
    }

    void commit() {
        this.status = ReservationStatus.COMMITTED;
    }
}
