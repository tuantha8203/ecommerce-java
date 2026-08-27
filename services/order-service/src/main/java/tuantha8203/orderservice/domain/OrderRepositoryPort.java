package tuantha8203.orderservice.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Optional<Order> findById(UUID id);
    Order save(Order order);
}