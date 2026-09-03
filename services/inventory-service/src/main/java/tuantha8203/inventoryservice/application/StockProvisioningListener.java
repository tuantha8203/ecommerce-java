package tuantha8203.inventoryservice.application;

import tuantha8203.inventoryservice.domain.Stock;
import tuantha8203.inventoryservice.events.ProductCreatedEvent;
import tuantha8203.inventoryservice.infrastructure.StockRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockProvisioningListener {

    private final StockRepository repository;

    public StockProvisioningListener(StockRepository repository) {
        this.repository = repository;
    }

    // Kafka chỉ đảm bảo at-least-once — product.created vẫn có thể bị gửi lại (retry publisher,
    // rebalance trước khi commit offset...) dù về nghiệp vụ chỉ xảy ra đúng 1 lần. Giữ check
    // existsById để lần gửi lại không ghi đè available_qty đã restock về lại 0. Không set số lượng
    // thật ở đây — luôn = 0, nhập kho thật đi qua StockService.restock() (endpoint riêng), tránh 2
    // đường ghi cạnh tranh vào cùng field với reserve()/release().
    @Transactional
    @KafkaListener(topics = "product.created", groupId = "inventory-service")
    public void onProductCreated(ProductCreatedEvent event) {
        if (!repository.existsById(event.productId())) {
            repository.save(new Stock(event.productId(), 0));
        }
    }
}
