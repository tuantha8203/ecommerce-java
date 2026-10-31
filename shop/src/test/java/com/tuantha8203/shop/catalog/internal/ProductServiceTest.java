package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.shared.PreconditionFailedException;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;

    @Mock
    CategoryRepository categories;

    @InjectMocks
    ProductService service;

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache productPages;

    private final Category shirts = new Category("Shirts", "shirts", null);

    private CreateProductRequest request(String sku) {
        return new CreateProductRequest(1L, sku, "T-shirt", "t-shirt", null, 150_000);
    }

    @Test
    void createsProductInDraftStatus() {
        when(categories.findById(1L)).thenReturn(Optional.of(shirts));
        when(products.existsBySku("SKU-1")).thenReturn(false);
        when(products.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDetails created = service.create(request("SKU-1"));

        assertThat(created.sku()).isEqualTo("SKU-1");
        assertThat(created.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(created.categoryName()).isEqualTo("Shirts");
    }

    @Test
    void rejectsDuplicateSkuWithoutSaving() {
        when(categories.findById(1L)).thenReturn(Optional.of(shirts));
        when(products.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("SKU-1"))).isInstanceOf(DuplicateSkuException.class);
        verify(products, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnknownCategory() {
        when(categories.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("SKU-1"))).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void publishedLookupHidesDraftProducts() {
        Product draft = new Product(shirts, "SKU-1", "T-shirt", "t-shirt", null, 150_000);
        when(products.findBySlug("t-shirt")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.getPublishedBySlug("t-shirt")).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateChangesDetailsWhenVersionMatches() {
        Product product = new Product(shirts, "SKU-1", "T-shirt", "t-shirt", null, 150_000);
        when(products.findById(10L)).thenReturn(Optional.of(product));
        when(cacheManager.getCache(CatalogCacheConfig.PRODUCT_PAGES)).thenReturn(productPages);

        ProductDetails updated = service.update(10L, 0L, new UpdateProductRequest("Polo shirt", "Cotton", 199_000));

        assertThat(updated.name()).isEqualTo("Polo shirt");
        assertThat(updated.price()).isEqualTo(199_000);
        verify(productPages).evict("t-shirt"); // the public page must not keep showing the old price
    }

    @Test
    void updateRejectsStaleVersion() {
        Product product = new Product(shirts, "SKU-1", "T-shirt", "t-shirt", null, 150_000);
        when(products.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.update(10L, 5L, new UpdateProductRequest("Polo shirt", null, 199_000)))
                .isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void translatesSlugConstraintViolationIntoBusinessException() {
        when(categories.findById(1L)).thenReturn(Optional.of(shirts));
        when(products.existsBySku("SKU-1")).thenReturn(false);
        when(products.saveAndFlush(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key",
                        new ConstraintViolationException("duplicate key", new SQLException(), "uq_products_slug")));

        assertThatThrownBy(() -> service.create(request("SKU-1"))).isInstanceOf(DuplicateProductSlugException.class);
    }

    @Test
    void leavesUnknownConstraintViolationsUntouched() {
        when(categories.findById(1L)).thenReturn(Optional.of(shirts));
        when(products.existsBySku("SKU-1")).thenReturn(false);
        when(products.saveAndFlush(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "fk", new ConstraintViolationException("fk", new SQLException(), "some_other_constraint")));

        assertThatThrownBy(() -> service.create(request("SKU-1"))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
