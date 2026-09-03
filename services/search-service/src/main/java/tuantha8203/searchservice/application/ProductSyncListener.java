package tuantha8203.searchservice.application;

import tuantha8203.searchservice.domain.ProductDocument;
import tuantha8203.searchservice.events.ProductCreatedEvent;
import tuantha8203.searchservice.events.ProductDeletedEvent;
import tuantha8203.searchservice.events.ProductUpdatedEvent;
import tuantha8203.searchservice.infrastructure.ProductSearchRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ProductSyncListener {

    private final ProductSearchRepository repository;

    public ProductSyncListener(ProductSearchRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "product.created", groupId = "search-service")
    public void onProductCreated(ProductCreatedEvent event) {
        upsert(event.productId(), event.sku(), event.name(), event.description(), event.price());
    }

    @KafkaListener(topics = "product.updated", groupId = "search-service")
    public void onProductUpdated(ProductUpdatedEvent event) {
        upsert(event.productId(), event.sku(), event.name(), event.description(), event.price());
    }

    @KafkaListener(topics = "product.deleted", groupId = "search-service")
    public void onProductDeleted(ProductDeletedEvent event) {
        repository.deleteById(event.productId().toString());
    }

    private void upsert(UUID productId, String sku, String name, String description, BigDecimal price) {
        var doc = new ProductDocument();
        doc.setId(productId.toString());
        doc.setSku(sku);
        doc.setName(name);
        doc.setDescription(description);
        doc.setPrice(price);
        repository.save(doc);   // upsert theo id — ghi lại nhiều lần với cùng dữ liệu không gây lỗi
    }
}