package com.tuantha8203.shop.catalog.internal;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuantha8203.shop.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductSearchIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    // A random word added to every name: searches in this test only ever match this test's products
    private final String marker =
            "m" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private long categoryId;

    private String product(String name, String description, boolean active) {
        if (categoryId == 0) {
            categoryId = categoryService
                    .create(new CreateCategoryRequest("Search", "search-cat-" + marker, null))
                    .id();
        }
        String slug = "s-" + marker + "-" + UUID.randomUUID().toString().substring(0, 6);
        long id = productService
                .create(new CreateProductRequest(
                        categoryId,
                        "S-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
                        name + " " + marker,
                        slug,
                        description,
                        100_000))
                .id();
        if (active) {
            productService.activate(id);
        }
        return name + " " + marker;
    }

    private ResultActions search(String text) throws Exception {
        return mvc.perform(get("/api/v1/products/search").param("q", text + " " + marker));
    }

    @Test
    void findsVietnameseNamesTypedWithoutAccents() throws Exception {
        String tShirt = product("Áo thun cổ tròn", null, true);
        product("Quần jean", null, true);

        search("ao thun")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value(tShirt));
    }

    @Test
    void theLetterDWithStrokeMatchesAPlainD() throws Exception {
        String dress = product("Đầm dạ hội", null, true);

        search("dam da hoi").andExpect(jsonPath("$.items[0].name").value(dress));
    }

    @Test
    void aWordStillBeingTypedAlreadyMatches() throws Exception {
        String tShirt = product("Áo thun cổ tròn", null, true);

        search("ao th").andExpect(jsonPath("$.items[0].name").value(tShirt));
    }

    @Test
    void aMatchInTheNameRanksAboveAMatchInTheDescription() throws Exception {
        String cup = product("Cốc sứ", "Dùng thay bình nước khi đi làm", true);
        String bottle = product("Bình nước giữ nhiệt", null, true);

        search("binh nuoc")
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value(bottle))
                .andExpect(jsonPath("$.items[1].name").value(cup));
    }

    @Test
    void draftsAreNeverFound() throws Exception {
        String draft = product("Áo khoác chưa bán", null, false);

        search("ao khoac").andExpect(jsonPath("$.items[*].name").value(not(hasItem(draft))));
    }

    @Test
    void symbolsOnlyGiveAnEmptyResultNotAnError() throws Exception {
        mvc.perform(get("/api/v1/products/search").param("q", "!!! & ((("))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void anEmptyQueryIsRejected() throws Exception {
        mvc.perform(get("/api/v1/products/search").param("q", " ")).andExpect(status().isBadRequest());
    }

    @Test
    void anInvalidQueryParameterIsNamedInTheError() throws Exception {
        mvc.perform(get("/api/v1/products/search").param("q", "ao").param("page", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }
}
