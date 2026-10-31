package com.tuantha8203.shop.ordering.internal;

import static com.tuantha8203.shop.TestAuth.admin;
import static com.tuantha8203.shop.TestAuth.customer;
import static com.tuantha8203.shop.TestAuth.identity;
import static com.tuantha8203.shop.TestAuth.newSubject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.cart.internal.CartService;
import com.tuantha8203.shop.catalog.internal.CategoryService;
import com.tuantha8203.shop.catalog.internal.CreateCategoryRequest;
import com.tuantha8203.shop.catalog.internal.CreateProductRequest;
import com.tuantha8203.shop.catalog.internal.ProductService;
import com.tuantha8203.shop.catalog.internal.UpdateProductRequest;
import com.tuantha8203.shop.customer.internal.AddAddressRequest;
import com.tuantha8203.shop.customer.internal.CustomerIdentities;
import com.tuantha8203.shop.customer.internal.CustomerService;
import com.tuantha8203.shop.inventory.internal.InventoryService;
import com.tuantha8203.shop.payment.internal.PaymentService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderPlacementIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerIdentities identities;

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

    @MockitoBean
    PaymentService paymentService; // payment is tested separately; here orders stay PENDING_PAYMENT

    /** subject: who the token says the caller is. id: the customer profile behind it, for service calls. */
    record Buyer(String subject, long id) {}

    // --- test data helpers (test code may use other modules' internals) ---

    private Buyer buyer(boolean withAddress) {
        String subject = newSubject();
        long id = identities.resolve(identity(subject)); // the same profile the API will find from the token
        if (withAddress) {
            customerService.addAddress(
                    id, new AddAddressRequest("Buyer", "0900000001", "1 Le Loi", "Ben Thanh Ward", "Ho Chi Minh City"));
        }
        return new Buyer(subject, id);
    }

    private long product(String name, long price, int stock) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Ord", "ord-" + suffix, null))
                .id();
        long productId = productService
                .create(new CreateProductRequest(
                        categoryId, "ORD-" + suffix.toUpperCase(), name, "ord-item-" + suffix, null, price))
                .id();
        productService.activate(productId);
        inventoryService.setOnHand(productId, stock);
        return productId;
    }

    private ResultActions placeOrder(Buyer buyer, String key) throws Exception {
        return mvc.perform(
                post("/api/v1/orders").with(customer(buyer.subject())).header("Idempotency-Key", key));
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    // --- tests ---

    @Test
    void placesAnOrderFromTheCart() throws Exception {
        Buyer buyer = buyer(true);
        long shirt = product("Shirt", 100_000, 10);
        long socks = product("Socks", 50_000, 10);
        cartService.setQuantity(buyer.id(), shirt, 2);
        cartService.setQuantity(buyer.id(), socks, 1);

        placeOrder(buyer, newKey())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.total").value(250_000))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.shipping.street").value("1 Le Loi"));

        assertThat(cartService.lines(buyer.id())).isEmpty();
        assertThat(inventoryService.getStock(shirt).reserved()).isEqualTo(2);
    }

    @Test
    void sameKeyReturnsTheSameOrderWithoutReservingTwice() throws Exception {
        Buyer buyer = buyer(true);
        long shirt = product("Shirt", 100_000, 10);
        cartService.setQuantity(buyer.id(), shirt, 2);
        String key = newKey();

        String first = placeOrder(buyer, key)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String second = placeOrder(buyer, key)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat((Integer) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertThat(inventoryService.getStock(shirt).reserved()).isEqualTo(2);
    }

    @Test
    void insufficientStockLeavesNothingBehindAndTheKeyCanBeRetried() throws Exception {
        Buyer buyer = buyer(true);
        long shirt = product("Shirt", 100_000, 1);
        cartService.setQuantity(buyer.id(), shirt, 2);
        String key = newKey();

        placeOrder(buyer, key).andExpect(status().isConflict());

        assertThat(cartService.lines(buyer.id())).hasSize(1); // cart untouched
        assertThat(inventoryService.getStock(shirt).reserved()).isZero(); // nothing held
        mvc.perform(get("/api/v1/orders").with(customer(buyer.subject())))
                .andExpect(jsonPath("$.length()").value(0)); // no order

        inventoryService.setOnHand(shirt, 5); // the shop restocks
        placeOrder(buyer, key).andExpect(status().isCreated()); // same key now succeeds
    }

    @Test
    void lockedInPriceIgnoresLaterCatalogChanges() throws Exception {
        Buyer buyer = buyer(true);
        long shirt = product("Shirt", 100_000, 10);
        cartService.setQuantity(buyer.id(), shirt, 1);
        String json = placeOrder(buyer, newKey()).andReturn().getResponse().getContentAsString();
        int orderId = JsonPath.read(json, "$.id");

        long version = productService.getById(shirt).version();
        productService.update(shirt, version, new UpdateProductRequest("Shirt v2", null, 999_000));

        mvc.perform(get("/api/v1/orders/" + orderId).with(customer(buyer.subject())))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(100_000))
                .andExpect(jsonPath("$.lines[0].productName").value("Shirt"));
    }

    @Test
    void emptyCartIsRejected() throws Exception {
        placeOrder(buyer(true), newKey()).andExpect(status().isConflict());
    }

    @Test
    void missingAddressIsRejected() throws Exception {
        Buyer buyer = buyer(false);
        cartService.setQuantity(buyer.id(), product("Shirt", 100_000, 10), 1);

        placeOrder(buyer, newKey()).andExpect(status().isConflict());
    }

    @Test
    void missingIdempotencyKeyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/orders").with(customer(newSubject()))).andExpect(status().isBadRequest());
    }

    @Test
    void customersCannotSeeEachOthersOrders() throws Exception {
        Buyer buyer = buyer(true);
        cartService.setQuantity(buyer.id(), product("Shirt", 100_000, 10), 1);
        int orderId = JsonPath.read(
                placeOrder(buyer, newKey()).andReturn().getResponse().getContentAsString(), "$.id");

        Buyer someoneElse = buyer(true);
        mvc.perform(get("/api/v1/orders/" + orderId).with(customer(someoneElse.subject())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellingAPendingOrderReleasesTheStock() throws Exception {
        Buyer buyer = buyer(true);
        long shirt = product("Shirt", 100_000, 10);
        cartService.setQuantity(buyer.id(), shirt, 3);
        int orderId = JsonPath.read(
                placeOrder(buyer, newKey()).andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/v1/orders/" + orderId + "/cancellation").with(customer(buyer.subject())))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/orders/" + orderId).with(customer(buyer.subject())))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(inventoryService.getStock(shirt).reserved()).isZero();
    }

    @Test
    void anUnpaidOrderCannotBeShipped() throws Exception {
        Buyer buyer = buyer(true);
        cartService.setQuantity(buyer.id(), product("Shirt", 100_000, 10), 1);
        int orderId = JsonPath.read(
                placeOrder(buyer, newKey()).andReturn().getResponse().getContentAsString(), "$.id");

        // the admin is allowed to ship, but the order is not paid yet: 409, not 403
        mvc.perform(post("/api/v1/admin/orders/" + orderId + "/shipment").with(admin()))
                .andExpect(status().isConflict());
    }
}
