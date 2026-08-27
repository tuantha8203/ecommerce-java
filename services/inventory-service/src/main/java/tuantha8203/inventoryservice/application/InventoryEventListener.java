package tuantha8203.inventoryservice.application;

import tuantha8203.inventoryservice.events.InventoryFailedEvent;
import tuantha8203.inventoryservice.events.InventoryReservedEvent;
import tuantha8203.inventoryservice.events.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final StockService stockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String CONSUMER_GROUP_ID = "inventory-service";
    private static final String INVENTORY_FAILED_TOPIC = "inventory.failed";
    private static final String INVENTORY_RESERVED_TOPIC = "inventory.reserved";

    public InventoryEventListener(StockService stockService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.stockService = stockService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = ORDER_CREATED_TOPIC, groupId = CONSUMER_GROUP_ID)
    public void onOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
        OrderCreatedEvent event = record.value();
        List<OrderCreatedEvent.Item> reservedSoFar = new ArrayList<>();

        try {
            for (OrderCreatedEvent.Item item : event.items()) {
                boolean ok = stockService.reserve(item.productId(), item.quantity());
                if (!ok) {
                    // rollback các item đã được reserve thành công trước đó trong cùng order
                    reservedSoFar.forEach(i -> stockService.release(i.productId(), i.quantity()));
                    kafkaTemplate.send(INVENTORY_FAILED_TOPIC, event.orderId().toString(),
                                    new InventoryFailedEvent(event.orderId(), "Insufficient stock for product " + item.productId()))
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to publish inventory.failed for order {}", event.orderId(), ex);
                                }
                            });
                    return;
                }
                reservedSoFar.add(item);
            }
        } catch (RuntimeException ex) {
            // reserve() ném exception giữa chừng (vd LockAcquisitionFailedException) — release các item
            // đã reserve thành công trước khi rethrow để error handler retry không double-reserve chúng.
            reservedSoFar.forEach(i -> stockService.release(i.productId(), i.quantity()));
            throw ex;
        }

        kafkaTemplate.send(INVENTORY_RESERVED_TOPIC, event.orderId().toString(),
                        new InventoryReservedEvent(event.orderId()))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish inventory.reserved for order {}", event.orderId(), ex);
                    }
                });
    }
}