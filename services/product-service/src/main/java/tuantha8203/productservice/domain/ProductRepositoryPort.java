package tuantha8203.productservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
    Optional<Product> findById(UUID id);
    Page<Product> findAll(Pageable pageable);
    Product save(Product product);
    void delete(Product product);
}
