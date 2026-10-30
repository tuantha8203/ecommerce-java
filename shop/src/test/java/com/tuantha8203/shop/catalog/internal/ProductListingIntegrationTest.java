package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tuantha8203.shop.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductListingIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    private String categorySlug;
    private final List<String> activeSlugsNewestFirst = new ArrayList<>();

    @BeforeEach
    void createProducts() {
        // a unique category isolates this test from data created by other tests
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        categorySlug = "listing-" + suffix;
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Listing", categorySlug, null))
                .id();

        for (int i = 1; i <= 25; i++) {
            String slug = "item-" + suffix + "-" + i;
            ProductDetails product = productService.create(new CreateProductRequest(
                    categoryId, "LST-" + suffix.toUpperCase() + "-" + i, "Item " + i, slug, null, 1000L * i));
            productService.activate(product.id());
            activeSlugsNewestFirst.addFirst(slug);
        }
        // two drafts that must never appear on the public listing
        productService.create(new CreateProductRequest(
                categoryId, "LST-" + suffix.toUpperCase() + "-D1", "Draft 1", "draft-" + suffix + "-1", null, 1));
        productService.create(new CreateProductRequest(
                categoryId, "LST-" + suffix.toUpperCase() + "-D2", "Draft 2", "draft-" + suffix + "-2", null, 1));
    }

    @Test
    void cursorWalksEveryActiveProductExactlyOnceNewestFirst() throws Exception {
        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;

        do {
            MockHttpServletRequestBuilder request =
                    get("/api/v1/products").param("category", categorySlug).param("limit", "10");
            if (cursor != null) {
                request.param("after", cursor);
            }
            String json = mvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            List<String> slugs = JsonPath.read(json, "$.items[*].slug");
            seen.addAll(slugs);
            cursor = JsonPath.read(json, "$.nextCursor");
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3); // 10 + 10 + 5
        assertThat(seen).containsExactlyElementsOf(activeSlugsNewestFirst);
    }

    @Test
    void tamperedCursorIsRejected() throws Exception {
        mvc.perform(get("/api/v1/products").param("after", "not-a-valid-cursor"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limitAboveMaximumIsRejected() throws Exception {
        mvc.perform(get("/api/v1/products").param("limit", "500")).andExpect(status().isBadRequest());
    }

    @Test
    void adminListFiltersByStatusAndReportsTotals() throws Exception {
        mvc.perform(get("/api/v1/admin/products").param("status", "DRAFT").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.items[*].status")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("DRAFT"))))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }
}
