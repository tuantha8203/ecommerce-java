package tuantha8203.searchservice.application;

import tuantha8203.searchservice.domain.ProductDocument;
import tuantha8203.searchservice.events.ProductDeletedEvent;
import tuantha8203.searchservice.events.ProductUpdatedEvent;
import tuantha8203.searchservice.infrastructure.ProductSearchRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductSyncListener {

    private final ProductSearchRepository repository;

    public ProductSyncListener(ProductSearchRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "product.updated", groupId = "search-service")
    public void onProductUpdated(ProductUpdatedEvent event) {
        var doc = new ProductDocument();
        doc.setId(event.productId().toString());
        doc.setSku(event.sku());
        doc.setName(event.name());
        doc.setDescription(event.description());
        doc.setPrice(event.price());
        repository.save(doc);   // upsert theo id — ghi lại nhiều lần với cùng dữ liệu không gây lỗi
    }

    @KafkaListener(topics = "product.deleted", groupId = "search-service")
    public void onProductDeleted(ProductDeletedEvent event) {
        repository.deleteById(event.productId().toString());
    }
}