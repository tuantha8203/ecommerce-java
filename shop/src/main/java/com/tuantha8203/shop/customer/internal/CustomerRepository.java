package com.tuantha8203.shop.customer.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Viết lower(...) để khớp với index uq_customers_email_lower.
    // Không dùng findByEmailIgnoreCase: Spring Data sinh ra upper(...), không dùng được index.
    @Query("select c from Customer c where lower(c.email) = lower(:email)")
    Optional<Customer> findByEmail(@Param("email") String email);
}
