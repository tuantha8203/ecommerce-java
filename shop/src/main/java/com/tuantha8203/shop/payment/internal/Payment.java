package com.tuantha8203.shop.payment.internal;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "payment", name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderRef;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, length = 100)
    private String idempotencyKey;

    private String paymentToken;

    private String gatewayChargeId;

    private String failureReason;

    @Version
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    static Payment start(String orderRef, long amount, String paymentToken) {
        Payment payment = new Payment();
        payment.orderRef = orderRef;
        payment.amount = amount;
        payment.paymentToken = paymentToken;
        payment.status = PaymentStatus.PENDING;
        // Derived from the order, not random: even after a crash and restart,
        // every attempt for this order reuses the same key, so the gateway charges once
        payment.idempotencyKey = "pay-" + orderRef;
        return payment;
    }

    boolean isUnresolved() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.UNKNOWN;
    }

    void markUnknown() {
        if (status == PaymentStatus.PENDING) {
            status = PaymentStatus.UNKNOWN;
        }
    }

    /** Returns true only on the first resolution, so the caller publishes the event exactly once. */
    boolean succeed(String chargeId) {
        if (!isUnresolved()) {
            return false;
        }
        status = PaymentStatus.SUCCEEDED;
        gatewayChargeId = chargeId;
        return true;
    }

    boolean decline(String reason) {
        if (!isUnresolved()) {
            return false;
        }
        status = PaymentStatus.DECLINED;
        failureReason = reason;
        return true;
    }

    void requestRefund() {
        if (status == PaymentStatus.SUCCEEDED) {
            status = PaymentStatus.REFUND_REQUESTED;
        }
    }

    void markRefunded() {
        status = PaymentStatus.REFUNDED;
    }
}
