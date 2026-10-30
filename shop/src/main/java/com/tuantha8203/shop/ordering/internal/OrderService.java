package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.inventory.InventoryApi;
import com.tuantha8203.shop.payment.PaymentApi;
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
    private final PaymentApi payments;

    public PlacementResult placeOrder(long customerId, String idempotencyKey, String paymentToken) {
        IdempotencyKeys.Claim claim = idempotencyKeys.claim(customerId, idempotencyKey);
        switch (claim.state()) {
            case COMPLETED -> {
                return new PlacementResult(getOrder(customerId, claim.orderId()), false);
            }
            case IN_PROGRESS -> throw new RequestInProgressException();
            case CLAIMED -> {
                OrderView created;
                try {
                    created = placement.place(customerId, idempotencyKey);
                } catch (RuntimeException ex) {
                    idempotencyKeys.release(customerId, idempotencyKey);
                    throw ex;
                }
                // No transaction is open here: holding a database connection while waiting
                // for the gateway would exhaust the connection pool as soon as the gateway slows down
                payments.charge(Order.referenceOf(created.id()), created.total(), paymentToken);
                // PaymentEventsListener has already applied the outcome to the order: read its fresh state
                return new PlacementResult(getOrder(customerId, created.id()), true);
            }
        }
        throw new IllegalStateException("Unknown claim state: " + claim.state());
    }

    @Transactional
    public OrderView cancelByAdmin(long orderId, String reason) {
        Order order = load(orderId);
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.cancel(reason);
        inventory.release(order.reference());
        if (wasPaid) {
            payments.requestRefund(order.reference());
        }
        return OrderView.from(order);
    }

    /** Called by the expiry job, one order per transaction. */
    @Transactional
    public void expire(long orderId) {
        Order order = load(orderId);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) { // it may have been paid meanwhile
            order.cancel("Payment not completed in time");
            inventory.release(order.reference());
        }
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

    private Order loadOwned(long customerId, long orderId) {
        return orders.findByIdAndCustomerId(orderId, customerId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Order load(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
