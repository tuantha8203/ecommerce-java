package tuantha8203.productservice.infrastructure;

import tuantha8203.productservice.domain.Product;
import tuantha8203.productservice.events.ProductDeletedEvent;
import tuantha8203.productservice.events.ProductUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUpdated(Product product) {
        var event = new ProductUpdatedEvent(product.getId(), product.getSku(), product.getName(),
                product.getDescription(), product.getPrice());
        kafkaTemplate.send("product.updated", product.getId().toString(), event);
    }

    public void publishDeleted(java.util.UUID productId) {
        kafkaTemplate.send("product.deleted", productId.toString(), new ProductDeletedEvent(productId));
    }
}