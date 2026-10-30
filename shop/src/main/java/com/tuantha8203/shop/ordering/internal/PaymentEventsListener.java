package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.inventory.InventoryApi;
import com.tuantha8203.shop.payment.PaymentApi;
import com.tuantha8203.shop.payment.PaymentFailed;
import com.tuantha8203.shop.payment.PaymentSucceeded;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reacts to payment results. Runs synchronously inside the payment module's transaction,
 * so a payment and its order never disagree. Every branch is safe to run twice.
 */
@Component
@RequiredArgsConstructor
class PaymentEventsListener {

    private final OrderRepository orders;
    private final InventoryApi inventory;
    private final PaymentApi payments;

    @EventListener
    @Transactional
    void on(PaymentSucceeded event) {
        orders.findById(Order.idFromReference(event.orderRef())).ifPresent(order -> {
            switch (order.getStatus()) {
                case PENDING_PAYMENT -> {
                    if (inventory.confirm(order.reference())) {
                        order.markPaid();
                    } else {
                        // Paid, but the reservation expired in the meantime: we cannot ship it
                        order.cancel("Stock reservation expired before the payment completed");
                        inventory.release(order.reference());
                        payments.requestRefund(order.reference());
                    }
                }
                // Money arrived for an order already cancelled (customer cancel, timeout): give it back
                case CANCELLED -> payments.requestRefund(order.reference());
                // PAID, SHIPPED, DELIVERED: duplicate notification, nothing to do
                default -> {}
            }
        });
    }

    @EventListener
    @Transactional
    void on(PaymentFailed event) {
        orders.findById(Order.idFromReference(event.orderRef())).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.cancel("Payment failed: " + event.reason());
                inventory.release(order.reference());
            }
        });
    }
}
