package com.tuantha8203.shop.catalog.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tuantha8203.shop.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void productLifecycleFromCreationToPublicPage() throws Exception {
        // unique values so this test never collides with data from other tests
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "t-shirt-" + suffix;

        String categoryJson = mvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Shirts", "slug": "shirts-%s"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int categoryId = JsonPath.read(categoryJson, "$.id");

        String location = mvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": %d, "sku": "SKU-%s", "name": "T-shirt", "slug": "%s", "price": 150000}
                                """.formatted(categoryId, suffix.toUpperCase(), slug)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION);
        String productPath = location.substring(location.indexOf("/api/"));

        // a draft is invisible on the public page
        mvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isNotFound());

        mvc.perform(post(productPath + "/activation")).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/products/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // first update with the current version succeeds and bumps the version
        mvc.perform(put(productPath)
                        .header(HttpHeaders.IF_MATCH, "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Polo shirt", "price": 199000}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""));

        // a second update still holding the old version is rejected
        mvc.perform(put(productPath)
                        .header(HttpHeaders.IF_MATCH, "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Overwrite attempt", "price": 1}
                                """))
                .andExpect(status().isPreconditionFailed());
    }
}
