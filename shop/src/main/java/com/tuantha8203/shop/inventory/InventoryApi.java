package com.tuantha8203.shop.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventoryApi {

    /** Reserves every line or none of them. Calling it again with the same orderRef changes nothing. */
    Reservation reserve(String orderRef, List<ReservationLine> lines);

    /** The order shipped: reserved units leave the warehouse. */
    void commit(String orderRef);

    /** The order was cancelled or expired: reserved units become available again. Safe to call twice. */
    void release(String orderRef);

    /** Units that can still be sold, per product. Unknown products report 0. */
    Map<Long, Integer> availability(Collection<Long> productIds);

    /**
     * The order was paid: keep the units held with no expiry.
     * Returns false if the reservation already expired; the caller must then cancel and refund.
     */
    boolean confirm(String orderRef);
}
