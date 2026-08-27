package tuantha8203.orderservice.application;

import tuantha8203.orderservice.domain.OrderRepositoryPort;
import tuantha8203.orderservice.domain.OrderStatus;
import tuantha8203.orderservice.events.InventoryFailedEvent;
import tuantha8203.orderservice.events.InventoryReservedEvent;
import tuantha8203.orderservice.infrastructure.PaymentClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final OrderRepositoryPort repository;
    private final PaymentClient paymentClient;
    private final OrderStatusUpdater statusUpdater;

    private static final String CONSUMER_GROUP_ID = "order-service";
    private static final String INVENTORY_RESERVED_TOPIC = "inventory.reserved";
    private static final String INVENTORY_FAILED_TOPIC = "inventory.failed";

    public OrderEventListener(OrderRepositoryPort repository, PaymentClient paymentClient,
                               OrderStatusUpdater statusUpdater) {
        this.repository = repository;
        this.paymentClient = paymentClient;
        this.statusUpdater = statusUpdater;
    }

    @KafkaListener(topics = INVENTORY_RESERVED_TOPIC, groupId = CONSUMER_GROUP_ID)
    public void onInventoryReserved(InventoryReservedEvent event) {
        var order = repository.findById(event.orderId()).orElseThrow();
        boolean paid = paymentClient.charge(order.getId(), order.getTotalAmount());
        statusUpdater.updateStatus(event.orderId(), paid ? OrderStatus.CONFIRMED : OrderStatus.PAYMENT_FAILED);
        // TODO Phase 7: PAYMENT_FAILED phải trigger publish inventory.release (compensating transaction),
        // hiện tại mới chỉ đổi status, CHƯA rollback kho — sẽ sửa ở Phase 7.
    }

    @KafkaListener(topics = INVENTORY_FAILED_TOPIC, groupId = CONSUMER_GROUP_ID)
    public void onInventoryFailed(InventoryFailedEvent event) {
        statusUpdater.updateStatus(event.orderId(), OrderStatus.CANCELLED);
    }
}