package com.tuantha8203.shop.payment.internal;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderRef(String orderRef);

    // The synchronous response and a webhook may try to resolve the same payment at the same moment:
    // the row lock lets exactly one of them win
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderRef = :orderRef")
    Optional<Payment> lockByOrderRef(@Param("orderRef") String orderRef);

    List<Payment> findTop50ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            Collection<PaymentStatus> statuses, Instant before);

    List<Payment> findTop50ByStatusOrderByUpdatedAtAsc(PaymentStatus status);
}
