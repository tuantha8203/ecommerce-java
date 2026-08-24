package tuantha8203.productservice.application;

import tuantha8203.productservice.api.ProductRequest;
import tuantha8203.productservice.api.ProductResponse;
import tuantha8203.productservice.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    
    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        return product;
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getSku(),
            product.getPrice(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
    }
}
