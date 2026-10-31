package com.tuantha8203.shop.catalog.internal;

import static com.tuantha8203.shop.TestAuth.admin;
import static com.tuantha8203.shop.TestAuth.customer;
import static com.tuantha8203.shop.TestAuth.newSubject;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.shared.PreconditionFailedException;
import com.tuantha8203.shop.shared.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminProductController.class)
@Import(SecurityConfig.class) // slice tests do not load it on their own; without it the default rules apply
class AdminProductControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService products;

    @MockitoBean
    JwtDecoder jwtDecoder; // SecurityConfig needs one; jwt() bypasses it, so a mock is enough

    private static final String VALID_BODY = """
            {"categoryId": 1, "sku": "SKU-1", "name": "T-shirt", "slug": "t-shirt", "price": 150000}
            """;

    private static ProductDetails details(long id, long version) {
        return new ProductDetails(
                id,
                "SKU-1",
                "T-shirt",
                "t-shirt",
                null,
                150_000,
                ProductStatus.DRAFT,
                1L,
                "Shirts",
                List.of(),
                version);
    }

    @Test
    void createReturns201WithLocationAndEtag() throws Exception {
        when(products.create(any())).thenReturn(details(42L, 0));

        mvc.perform(post("/api/v1/admin/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/v1/admin/products/42")))
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    void customersCannotCreateProducts() throws Exception {
        mvc.perform(post("/api/v1/admin/products")
                        .with(customer(newSubject()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(products, never()).create(any());
    }

    @Test
    void createRejectsInvalidBodyWithoutCallingService() throws Exception {
        mvc.perform(post("/api/v1/admin/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "sku": "SKU-1", "name": "", "slug": "t-shirt", "price": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verify(products, never()).create(any());
    }

    @Test
    void unknownProductReturns404Problem() throws Exception {
        when(products.getById(99L)).thenThrow(new ProductNotFoundException("99"));

        mvc.perform(get("/api/v1/admin/products/99").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void updateWithoutIfMatchReturns400() throws Exception {
        mvc.perform(put("/api/v1/admin/products/1")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Polo", "price": 199000}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithStaleVersionReturns412() throws Exception {
        when(products.update(eq(1L), eq(3L), any())).thenThrow(new PreconditionFailedException("stale"));

        mvc.perform(put("/api/v1/admin/products/1")
                        .with(admin())
                        .header(HttpHeaders.IF_MATCH, "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Polo", "price": 199000}
                                """))
                .andExpect(status().isPreconditionFailed());
    }
}
