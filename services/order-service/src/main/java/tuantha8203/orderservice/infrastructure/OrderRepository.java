package tuantha8203.orderservice.infrastructure;

import tuantha8203.orderservice.domain.Order;
import tuantha8203.orderservice.domain.OrderRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderRepositoryPort {
}