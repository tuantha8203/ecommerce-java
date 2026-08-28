package tuantha8203.inventoryservice.application;

import tuantha8203.inventoryservice.domain.Stock;
import tuantha8203.inventoryservice.events.ProductUpdatedEvent;
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

    // product.updated bắn ra cả khi tạo lẫn khi sửa sản phẩm (Product Service tái dùng 1 event, xem
    // 6.9.1) — chỉ tạo dòng stock khi productId CHƯA tồn tại (lần đầu sản phẩm xuất hiện), bỏ qua khi
    // event tới từ 1 lần sửa sau đó. Không set available_qty thật ở đây, luôn = 0 — nhập kho thật đi qua
    // StockService.restock() (endpoint riêng), tránh 2 đường ghi cạnh tranh vào cùng field với
    // reserve()/release().
    @Transactional
    @KafkaListener(topics = "product.updated", groupId = "inventory-service")
    public void onProductUpdated(ProductUpdatedEvent event) {
        if (!repository.existsById(event.productId())) {
            repository.save(new Stock(event.productId(), 0));
        }
    }
}
