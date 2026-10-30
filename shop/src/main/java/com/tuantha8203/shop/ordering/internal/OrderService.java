package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.inventory.InventoryApi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    public record PlacementResult(OrderView order, boolean created) {}

    private final OrderRepository orders;
    private final OrderPlacement placement;
    private final IdempotencyKeys idempotencyKeys;
    private final InventoryApi inventory;

    /**
     * Deliberately NOT @Transactional: it coordinates several separate transactions
     * (claim the key, create the order, release the key on failure). In step 2.6 the
     * payment call will also go here, outside any transaction.
     */
    public PlacementResult placeOrder(long customerId, String idempotencyKey) {
        IdempotencyKeys.Claim claim = idempotencyKeys.claim(customerId, idempotencyKey);
        switch (claim.state()) {
            case COMPLETED -> {
                return new PlacementResult(getOrder(customerId, claim.orderId()), false);
            }
            case IN_PROGRESS -> throw new RequestInProgressException();
            case CLAIMED -> {
                try {
                    return new PlacementResult(placement.place(customerId, idempotencyKey), true);
                } catch (RuntimeException ex) {
                    idempotencyKeys.release(customerId, idempotencyKey);
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unknown claim state: " + claim.state());
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(long customerId, long orderId) {
        return OrderView.from(loadOwned(customerId, orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listOrders(long customerId) {
        return orders.findTop50ByCustomerIdOrderByCreatedAtDescIdDesc(customerId).stream()
                .map(OrderSummary::from)
                .toList();
    }

    /** Customers may cancel only while the order is still waiting for payment. */
    @Transactional
    public void cancelByCustomer(long customerId, long orderId) {
        Order order = loadOwned(customerId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalOrderTransitionException(orderId, order.getStatus(), OrderStatus.CANCELLED);
        }
        order.cancel("Cancelled by customer");
        inventory.release(order.reference());
    }

    @Transactional
    public OrderView ship(long orderId) {
        Order order = load(orderId);
        order.ship(); // throws unless the order is PAID
        inventory.commit(order.reference());
        return OrderView.from(order);
    }

    @Transactional
    public OrderView deliver(long orderId) {
        Order order = load(orderId);
        order.deliver();
        return OrderView.from(order);
    }

    @Transactional
    public OrderView cancelByAdmin(long orderId, String reason) {
        Order order = load(orderId);
        order.cancel(reason);
        inventory.release(order.reference());
        // step 2.6: a PAID order also needs a refund here
        return OrderView.from(order);
    }

    private Order loadOwned(long customerId, long orderId) {
        return orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Order load(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
