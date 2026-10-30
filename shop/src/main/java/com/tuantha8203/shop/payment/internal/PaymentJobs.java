package com.tuantha8203.shop.payment.internal;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentJobs {

    private final PaymentService payments;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${ecommerce-java.payment.reconcile-interval:PT30S}")
    void reconcile() {
        payments.reconcile(clock.instant());
    }

    @Scheduled(fixedDelayString = "${ecommerce-java.payment.refund-interval:PT30S}")
    void refunds() {
        payments.processRefunds();
    }
}
