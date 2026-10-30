package com.tuantha8203.shop.inventory.internal;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class ReservationExpiryJob {

    private final InventoryService inventory;
    private final InventoryProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${shopx.inventory.expiry-check-interval:PT30S}")
    void releaseExpiredReservations() {
        int total = 0;
        int released;
        do {
            // each batch is its own short transaction: locks are held briefly
            released = inventory.releaseExpired(clock.instant(), properties.expiryBatchSize());
            total += released;
        } while (released == properties.expiryBatchSize());

        if (total > 0) {
            log.info("Released {} expired stock reservations", total);
        }
    }
}
