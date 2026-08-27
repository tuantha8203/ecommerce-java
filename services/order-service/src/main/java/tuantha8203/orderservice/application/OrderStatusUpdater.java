package tuantha8203.orderservice.application;

import tuantha8203.orderservice.domain.OrderRepositoryPort;
import tuantha8203.orderservice.domain.OrderStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class OrderStatusUpdater {

    private final OrderRepositoryPort repository;

    OrderStatusUpdater(OrderRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    void updateStatus(UUID orderId, OrderStatus status) {
        var order = repository.findById(orderId).orElseThrow();
        order.setStatus(status);
    }
}