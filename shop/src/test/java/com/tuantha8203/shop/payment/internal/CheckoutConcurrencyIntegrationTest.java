package com.tuantha8203.shop.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.cart.internal.CartService;
import com.tuantha8203.shop.catalog.internal.CategoryService;
import com.tuantha8203.shop.catalog.internal.CreateCategoryRequest;
import com.tuantha8203.shop.catalog.internal.CreateProductRequest;
import com.tuantha8203.shop.catalog.internal.ProductService;
import com.tuantha8203.shop.customer.internal.AddAddressRequest;
import com.tuantha8203.shop.customer.internal.CustomerService;
import com.tuantha8203.shop.customer.internal.RegisterCustomerRequest;
import com.tuantha8203.shop.inventory.InsufficientStockException;
import com.tuantha8203.shop.inventory.internal.InventoryService;
import com.tuantha8203.shop.ordering.internal.OrderService;
import com.tuantha8203.shop.ordering.internal.OrderStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The whole checkout under load: idempotency, stock, payment and order state together. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CheckoutConcurrencyIntegrationTest {

    @MockitoBean
    GatewayClient gateway;

    @Autowired
    OrderService orderService;

    @Autowired
    CustomerService customerService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    CartService cartService;

    @Test
    void hundredBuyersRacingForTenUnitsYieldExactlyTenPaidOrders() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Race", "race-" + suffix, null))
                .id();
        long productId = productService
                .create(new CreateProductRequest(
                        categoryId, "RACE-" + suffix.toUpperCase(), "Limited shirt", "race-" + suffix, null, 100_000))
                .id();
        productService.activate(productId);
        inventoryService.setOnHand(productId, 10);

        when(gateway.charge(any(), any(), anyLong(), any()))
                .thenAnswer(call -> new GatewayClient.ChargeResponse(
                        "ch_" + UUID.randomUUID(), call.getArgument(1), call.getArgument(2), "SUCCEEDED", null));

        // 100 different customers, each with an address and the shirt in the cart
        int buyers = 100;
        List<Long> customerIds = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            long id = customerService
                    .register(new RegisterCustomerRequest("race-" + suffix + "-" + i + "@mail.com", "Racer " + i, null))
                    .id();
            customerService.addAddress(id, new AddAddressRequest("Racer", "0900000001", "1 Le Loi", "Ward", "HCMC"));
            cartService.setQuantity(id, productId, 1);
            customerIds.add(id);
        }

        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger paid = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Future<Object>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (long customerId : customerIds) {
                results.add(pool.submit(() -> {
                    startSignal.await();
                    try {
                        OrderService.PlacementResult result = orderService.placeOrder(
                                customerId, UUID.randomUUID().toString(), "tok_visa");
                        if (result.order().status() == OrderStatus.PAID) {
                            paid.incrementAndGet();
                        }
                    } catch (InsufficientStockException ex) {
                        soldOut.incrementAndGet();
                    }
                    return null;
                }));
            }
            startSignal.countDown(); // everyone clicks "place order" at the same moment
            for (Future<Object> result : results) {
                result.get(120, TimeUnit.SECONDS); // rethrows anything unexpected, e.g. a deadlock
            }
        }

        assertThat(paid).hasValue(10);
        assertThat(soldOut).hasValue(90);
        assertThat(inventoryService.getStock(productId).reserved()).isEqualTo(10);
        // the gateway was asked to charge exactly the ten successful orders, never the ninety others
        verify(gateway, times(10)).charge(any(), any(), anyLong(), any());
    }
}
