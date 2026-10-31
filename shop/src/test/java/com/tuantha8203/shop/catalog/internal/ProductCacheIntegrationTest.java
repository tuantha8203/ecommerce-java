package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuantha8203.shop.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductCacheIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    CacheManager cacheManager;

    private Cache productPages() {
        return cacheManager.getCache(CatalogCacheConfig.PRODUCT_PAGES);
    }

    /** Returns {id, slug}. */
    private Object[] product(boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Cache", "cache-" + suffix, null))
                .id();
        String slug = "cached-" + suffix;
        long id = productService
                .create(new CreateProductRequest(
                        categoryId, "CACHE-" + suffix.toUpperCase(), "Cached shirt", slug, null, 100_000))
                .id();
        if (active) {
            productService.activate(id);
        }
        return new Object[] {id, slug};
    }

    @Test
    void firstViewFillsTheCacheAndAPriceChangeEmptiesIt() throws Exception {
        Object[] p = product(true);
        long id = (long) p[0];
        String slug = (String) p[1];

        mvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isOk());
        assertThat(productPages().get(slug)).as("cached after the first view").isNotNull();

        long version = productService.getById(id).version();
        productService.update(id, version, new UpdateProductRequest("Cached shirt", null, 120_000));
        assertThat(productPages().get(slug))
                .as("evicted once the update committed")
                .isNull();

        mvc.perform(get("/api/v1/products/" + slug))
                .andExpect(jsonPath("$.price").value(120_000));
    }

    @Test
    void archivingRemovesThePageImmediately() throws Exception {
        Object[] p = product(true);
        String slug = (String) p[1];
        mvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isOk());

        productService.archive((long) p[0]);

        mvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void draftsAreNeverCached() throws Exception {
        String slug = (String) product(false)[1];

        mvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isNotFound());

        assertThat(productPages().get(slug)).isNull();
    }

    @Test
    void unchangedPageAnswers304WithoutABody() throws Exception {
        String slug = (String) product(true)[1];

        String etag = mvc.perform(get("/api/v1/products/" + slug))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=60, public"))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.ETAG);

        mvc.perform(get("/api/v1/products/" + slug).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }
}
