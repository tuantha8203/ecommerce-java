package com.tuantha8203.shop.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.catalog.internal.CategoryService;
import com.tuantha8203.shop.catalog.internal.CreateCategoryRequest;
import com.tuantha8203.shop.catalog.internal.CreateProductRequest;
import com.tuantha8203.shop.catalog.internal.ProductService;
import com.tuantha8203.shop.inventory.InsufficientStockException;
import com.tuantha8203.shop.inventory.ReservationLine;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InventoryIntegrationTest {

    @Autowired
    InventoryService inventory;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    Clock clock;

    /** Test-only shortcut through catalog's services to get a real product id. */
    private long newProductWithStock(int onHand) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Inv", "inv-" + suffix, null))
                .id();
        long productId = productService
                .create(new CreateProductRequest(
                        categoryId, "INV-" + suffix.toUpperCase(), "Item", "inv-item-" + suffix, null, 1000))
                .id();
        inventory.setOnHand(productId, onHand);
        return productId;
    }

    private static String newOrderRef() {
        return "order-" + UUID.randomUUID();
    }

    @Test
    void hundredConcurrentBuyersNeverOversellTenUnits() throws Exception {
        long productId = newProductWithStock(10);
        int buyers = 100;
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<Object>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < buyers; i++) {
                results.add(pool.submit(() -> {
                    startSignal.await(); // every thread waits here, then all start at the same moment
                    try {
                        inventory.reserve(newOrderRef(), List.of(new ReservationLine(productId, 1)));
                        succeeded.incrementAndGet();
                    } catch (InsufficientStockException ex) {
                        rejected.incrementAndGet();
                    }
                    return null;
                }));
            }
            startSignal.countDown();
            for (Future<Object> result : results) {
                result.get(60, TimeUnit.SECONDS); // rethrows any unexpected exception from a thread
            }
        }

        assertThat(succeeded).hasValue(10);
        assertThat(rejected).hasValue(90);
        assertThat(inventory.getStock(productId).reserved()).isEqualTo(10);
    }

    @Test
    void reservationIsAllOrNothing() {
        long inStock = newProductWithStock(5);
        long soldOut = newProductWithStock(0);

        assertThatThrownBy(() -> inventory.reserve(
                        newOrderRef(), List.of(new ReservationLine(inStock, 1), new ReservationLine(soldOut, 1))))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(inventory.getStock(inStock).reserved()).isZero();
    }

    @Test
    void reservingTwiceForTheSameOrderHoldsStockOnce() {
        long productId = newProductWithStock(5);
        String orderRef = newOrderRef();

        inventory.reserve(orderRef, List.of(new ReservationLine(productId, 2)));
        inventory.reserve(orderRef, List.of(new ReservationLine(productId, 2)));

        assertThat(inventory.getStock(productId).reserved()).isEqualTo(2);
    }

    @Test
    void releaseReturnsStockAndIsSafeToRepeat() {
        long productId = newProductWithStock(5);
        String orderRef = newOrderRef();
        inventory.reserve(orderRef, List.of(new ReservationLine(productId, 2)));

        inventory.release(orderRef);
        inventory.release(orderRef);

        StockView stock = inventory.getStock(productId);
        assertThat(stock.reserved()).isZero();
        assertThat(stock.available()).isEqualTo(5);
    }

    @Test
    void commitTakesUnitsOutOfTheWarehouse() {
        long productId = newProductWithStock(5);
        String orderRef = newOrderRef();
        inventory.reserve(orderRef, List.of(new ReservationLine(productId, 2)));

        inventory.commit(orderRef);

        StockView stock = inventory.getStock(productId);
        assertThat(stock.onHand()).isEqualTo(3);
        assertThat(stock.reserved()).isZero();
    }

    @Test
    void expiredReservationsAreReleased() {
        long productId = newProductWithStock(5);
        inventory.reserve(newOrderRef(), List.of(new ReservationLine(productId, 3)));

        // pretend we are one hour in the future: the 15-minute reservation has expired
        inventory.releaseExpired(clock.instant().plus(Duration.ofHours(1)), 1000);

        assertThat(inventory.getStock(productId).reserved()).isZero();
    }

    @Test
    void stockCannotBeSetBelowWhatIsReserved() {
        long productId = newProductWithStock(5);
        inventory.reserve(newOrderRef(), List.of(new ReservationLine(productId, 3)));

        assertThatThrownBy(() -> inventory.setOnHand(productId, 2)).isInstanceOf(StockBelowReservedException.class);
    }
}
