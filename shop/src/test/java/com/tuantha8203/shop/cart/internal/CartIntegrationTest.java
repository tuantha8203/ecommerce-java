package com.tuantha8203.shop.cart.internal;

import static com.tuantha8203.shop.TestAuth.customer;
import static com.tuantha8203.shop.TestAuth.newSubject;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.catalog.internal.CategoryService;
import com.tuantha8203.shop.catalog.internal.CreateCategoryRequest;
import com.tuantha8203.shop.catalog.internal.CreateProductRequest;
import com.tuantha8203.shop.catalog.internal.ProductService;
import com.tuantha8203.shop.catalog.internal.UpdateProductRequest;
import com.tuantha8203.shop.inventory.internal.InventoryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CartIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    InventoryService inventoryService;

    // JUnit creates a new instance per test, so every test gets its own account and its own cart
    private final String subject = newSubject();

    private long product(long price, int stock, boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Cart", "cart-" + suffix, null))
                .id();
        long productId = productService
                .create(new CreateProductRequest(
                        categoryId,
                        "CART-" + suffix.toUpperCase(),
                        "Item " + suffix,
                        "cart-item-" + suffix,
                        null,
                        price))
                .id();
        if (active) {
            productService.activate(productId);
        }
        inventoryService.setOnHand(productId, stock);
        return productId;
    }

    private ResultActions setQuantity(long productId, int quantity) throws Exception {
        return mvc.perform(put("/api/v1/cart/items/" + productId)
                .with(customer(subject))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": %d}".formatted(quantity)));
    }

    private ResultActions getCart() throws Exception {
        return mvc.perform(get("/api/v1/cart").with(customer(subject)));
    }

    @Test
    void computesTotalsFromCurrentPrices() throws Exception {
        long shirt = product(100_000, 10, true);
        long socks = product(50_000, 10, true);

        setQuantity(shirt, 2).andExpect(status().isOk());
        setQuantity(socks, 1).andExpect(status().isOk());

        getCart()
                .andExpect(jsonPath("$.itemCount").value(3))
                .andExpect(jsonPath("$.total").value(250_000))
                .andExpect(jsonPath("$.checkoutReady").value(true));
    }

    @Test
    void settingQuantityTwiceReplacesInsteadOfAdding() throws Exception {
        long shirt = product(100_000, 10, true);

        setQuantity(shirt, 2);
        setQuantity(shirt, 5)
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].quantity").value(5));
    }

    @Test
    void removingAnItemIsSafeToRepeat() throws Exception {
        long shirt = product(100_000, 10, true);
        setQuantity(shirt, 1);

        for (int i = 0; i < 2; i++) {
            mvc.perform(delete("/api/v1/cart/items/" + shirt).with(customer(subject)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lines.length()").value(0));
        }
    }

    @Test
    void totalFollowsPriceChangesInTheCatalog() throws Exception {
        long shirt = product(100_000, 10, true);
        setQuantity(shirt, 2);

        long version = productService.getById(shirt).version();
        productService.update(shirt, version, new UpdateProductRequest("Shirt", null, 120_000));

        getCart().andExpect(jsonPath("$.total").value(240_000));
    }

    @Test
    void flagsLinesThatExceedAvailableStock() throws Exception {
        long shirt = product(100_000, 1, true);

        setQuantity(shirt, 3)
                .andExpect(status().isOk()) // the cart accepts it...
                .andExpect(jsonPath("$.lines[0].purchasable").value(false)) // ...but marks it
                .andExpect(jsonPath("$.lines[0].availableStock").value(1))
                .andExpect(jsonPath("$.checkoutReady").value(false));
    }

    @Test
    void rejectsProductsThatAreNotForSale() throws Exception {
        long draft = product(100_000, 10, false);

        setQuantity(draft, 1).andExpect(status().isConflict());
    }

    @Test
    void rejectsUnknownProducts() throws Exception {
        setQuantity(999_999_999L, 1).andExpect(status().isNotFound());
    }

    @Test
    void rejectsQuantitiesOutOfRange() throws Exception {
        long shirt = product(100_000, 10, true);

        setQuantity(shirt, 0).andExpect(status().isBadRequest());
        setQuantity(shirt, 100).andExpect(status().isBadRequest());
    }

    @Test
    void cartsBelongToOneCustomerOnly() throws Exception {
        long shirt = product(100_000, 10, true);
        setQuantity(shirt, 1);

        // a different account sees its own, empty cart
        mvc.perform(get("/api/v1/cart").with(customer(newSubject())))
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    void theCartNeedsALoggedInCustomer() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
    }
}
