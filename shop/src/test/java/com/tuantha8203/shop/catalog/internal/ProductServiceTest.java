package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tuantha8203.shop.catalog.ProductStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;

    @Mock
    CategoryRepository categories;

    @InjectMocks
    ProductService service;

    private final Category shirts = new Category("Shirts", "shirts", null);

    private CreateProductRequest request(String sku) {
        return new CreateProductRequest(1L, sku, "T-shirt", "t-shirt", null, 150_000);
    }

    @Test
    void createsProductInDraftStatus() {
        when(categories.findById(1L)).thenReturn(Optional.of(shirts));
        when(products.existsBySku("SKU-1")).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        verify(products, never()).save(any());
    }

    @Test
    void rejectsUnknownCategory() {
        when(categories.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("SKU-1"))).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void getBySlugThrowsWhenProductDoesNotExist() {
        when(products.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("missing")).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateChangesNameDescriptionAndPrice() {
        Product product = new Product(shirts, "SKU-1", "T-shirt", "t-shirt", null, 150_000);
        when(products.findById(10L)).thenReturn(Optional.of(product));

        ProductDetails updated = service.update(10L, new UpdateProductRequest("Polo shirt", "Cotton", 199_000));

        assertThat(updated.name()).isEqualTo("Polo shirt");
        assertThat(updated.price()).isEqualTo(199_000);
    }
}
