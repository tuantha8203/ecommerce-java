package com.tuantha8203.shop.ordering.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// "Order" is a keyword in JPQL (ORDER BY), so the entity gets a different name in queries
@Entity(name = "CustomerOrder")
@Table(schema = "ordering", name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private long total;

    @Embedded
    private ShippingDetails shipping;

    private String cancelReason;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    static Order place(long customerId, ShippingDetails shipping) {
        Order order = new Order();
        order.customerId = customerId;
        order.shipping = shipping;
        order.status = OrderStatus.PENDING_PAYMENT;
        return order;
    }

    void addLine(long productId, String productName, long unitPrice, int quantity) {
        OrderLine line = new OrderLine(this, productId, productName, unitPrice, quantity);
        lines.add(line);
        total += line.lineTotal();
    }

    /** The reference other modules (inventory, payment) know this order by. */
    public String reference() {
        return "order-" + id;
    }

    void markPaid() {
        transitionTo(OrderStatus.PAID);
    }

    void ship() {
        transitionTo(OrderStatus.SHIPPED);
    }

    void deliver() {
        transitionTo(OrderStatus.DELIVERED);
    }

    void cancel(String reason) {
        transitionTo(OrderStatus.CANCELLED);
        this.cancelReason = reason;
    }

    private void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalOrderTransitionException(id, status, next);
        }
        this.status = next;
    }
}
