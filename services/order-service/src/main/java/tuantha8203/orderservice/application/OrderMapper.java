package tuantha8203.orderservice.application;

import tuantha8203.orderservice.api.OrderRequest;
import tuantha8203.orderservice.api.OrderResponse;
import tuantha8203.orderservice.domain.Order;
import tuantha8203.orderservice.domain.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequest request) {
        Order order = new Order();
        request.items().forEach(i -> order.addItem(new OrderItem(i.productId(), i.quantity(), i.unitPrice())));
        return order;
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream()
                        .map(i -> new OrderResponse.Item(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}