package com.tuantha8203.shop.ordering.internal;

import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrderExpiryJob {

    private final OrderRepository orders;
    private final OrderService orderService;
    private final Clock clock;

    @Value("${ecommerce-java.ordering.payment-timeout:PT30M}")
    private Duration paymentTimeout;

    @Scheduled(fixedDelayString = "${ecommerce-java.ordering.expiry-check-interval:PT1M}")
    void cancelUnpaidOrders() {
        orders.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        OrderStatus.PENDING_PAYMENT, clock.instant().minus(paymentTimeout))
                .forEach(order -> orderService.expire(order.getId()));
    }
}
