package com.tuantha8203.shop.inventory.internal;

import com.tuantha8203.shop.catalog.CatalogApi;
import com.tuantha8203.shop.inventory.InsufficientStockException;
import com.tuantha8203.shop.inventory.InventoryApi;
import com.tuantha8203.shop.inventory.Reservation;
import com.tuantha8203.shop.inventory.ReservationLine;
import com.tuantha8203.shop.shared.ConstraintViolations;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService implements InventoryApi {

    private final StockItemRepository stockItems;
    private final StockReservationRepository reservations;
    private final CatalogApi catalog;
    private final InventoryProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public Reservation reserve(String orderRef, List<ReservationLine> lines) {
        List<StockReservation> existing = reservations.findByOrderRef(orderRef);
        if (!existing.isEmpty()) {
            // Retried call for the same order: do not reserve twice
            return new Reservation(orderRef, existing.getFirst().getExpiresAt());
        }

        Instant expiresAt = clock.instant().plus(properties.reservationTtl());

        // Merge duplicate products and sort by product id. Every transaction then locks rows
        // in the same order, so two orders for products A and B can never deadlock each other.
        Map<Long, Integer> quantities = lines.stream()
                .collect(Collectors.toMap(
                        ReservationLine::productId, ReservationLine::quantity, Integer::sum, TreeMap::new));

        quantities.forEach((productId, quantity) -> {
            if (stockItems.tryReserve(productId, quantity) == 0) {
                // Throwing rolls back the whole transaction, including lines reserved before this one
                throw new InsufficientStockException(productId, quantity);
            }
            reservations.save(new StockReservation(orderRef, productId, quantity, expiresAt));
        });
        return new Reservation(orderRef, expiresAt);
    }

    @Override
    @Transactional
    public void commit(String orderRef) {
        reservations.findByOrderRef(orderRef).stream()
                .filter(StockReservation::isActive)
                .forEach(reservation -> {
                    stockItems.commit(reservation.getProductId(), reservation.getQuantity());
                    reservation.commit();
                });
    }

    @Override
    @Transactional
    public void release(String orderRef) {
        reservations.findByOrderRef(orderRef).stream()
                .filter(StockReservation::isActive) // already released or committed: nothing to do
                .forEach(this::releaseOne);
    }

    @Override
    public Map<Long, Integer> availability(Collection<Long> productIds) {
        Map<Long, Integer> available = stockItems.findAllById(productIds).stream()
                .collect(Collectors.toMap(StockItem::getProductId, StockItem::available));
        productIds.forEach(id -> available.putIfAbsent(id, 0));
        return available;
    }

    /** Releases one batch of expired reservations; returns how many were released. */
    @Transactional
    public int releaseExpired(Instant now, int batchSize) {
        List<StockReservation> expired = reservations.lockExpired(now, batchSize);
        expired.forEach(this::releaseOne);
        return expired.size();
    }

    @Transactional
    public StockView setOnHand(long productId, int onHand) {
        if (catalog.findProductsByIds(List.of(productId)).isEmpty()) {
            throw new UnknownProductException(productId);
        }
        try {
            stockItems.upsertOnHand(productId, onHand);
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.constraintName(ex)
                    .filter("ck_stock_items_quantities"::equals)
                    .isPresent()) {
                throw new StockBelowReservedException(productId, onHand);
            }
            throw ex;
        }
        return getStock(productId);
    }

    public StockView getStock(long productId) {
        return stockItems.findById(productId).map(StockView::from).orElseGet(() -> StockView.empty(productId));
    }

    private void releaseOne(StockReservation reservation) {
        stockItems.release(reservation.getProductId(), reservation.getQuantity());
        reservation.release();
    }
}
