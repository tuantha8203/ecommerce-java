package com.tuantha8203.shop.ordering.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<Order, Long> {

    /** Scoped by customer: someone else's order id simply "does not exist". */
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findByIdAndCustomerId(long id, long customerId);

    @Override
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findById(Long id);

    // No lines here: fetching a collection together with a row limit is what we made fail on purpose
    List<Order> findTop50ByCustomerIdOrderByCreatedAtDescIdDesc(long customerId);

    List<Order> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus status, Instant before);
}
