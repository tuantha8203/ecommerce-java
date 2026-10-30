package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.catalog.ProductStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
