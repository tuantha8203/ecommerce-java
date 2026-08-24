package tuantha8203.productservice.infrastructure;

import tuantha8203.productservice.domain.Product;
import tuantha8203.productservice.domain.ProductRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductRepositoryPort {
}
