package tuantha8203.orderservice.application;

import tuantha8203.orderservice.api.OrderRequest;
import tuantha8203.orderservice.api.OrderResponse;
import tuantha8203.orderservice.domain.Order;
import tuantha8203.orderservice.domain.OrderRepositoryPort;
import tuantha8203.orderservice.domain.OrderStatus;
import tuantha8203.orderservice.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepositoryPort repository;
    private final OrderMapper mapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_CREATED_TOPIC = "order.created";

    public OrderService(OrderRepositoryPort repository, OrderMapper mapper, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public OrderResponse createOrder(OrderRequest request) {
        Order order = mapper.toEntity(request);
        BigDecimal total = order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(total);
        Order saved = repository.save(order);

        var event = new OrderCreatedEvent(
            saved.getId(),
            saved.getItems().stream()
                    .map(i -> new OrderCreatedEvent.Item(i.getProductId(), i.getQuantity()))
                    .toList(),
            saved.getTotalAmount()
        );

        kafkaTemplate.send(ORDER_CREATED_TOPIC, saved.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order.created for order {}", saved.getId(), ex);
                    }
                });

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return mapper.toResponse(repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id)));
    }
}