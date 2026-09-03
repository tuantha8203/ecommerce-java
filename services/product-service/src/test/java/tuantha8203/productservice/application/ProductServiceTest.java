package tuantha8203.productservice.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import tuantha8203.productservice.api.ProductRequest;
import tuantha8203.productservice.domain.Product;
import tuantha8203.productservice.domain.ProductRepositoryPort;
import tuantha8203.productservice.infrastructure.ProductEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepositoryPort repository;

    @Mock
    ProductEventPublisher eventPublisher;

    ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, new ProductMapper(), eventPublisher);
    }

    // Product.id chỉ gán được qua @GeneratedValue lúc persist thật — test cần set tay 1 id
    // xác định để assert, nên dùng reflection thay vì đòi entity có setter công khai (4.3).
    private Product product(UUID id, String sku, String name, BigDecimal price) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription("desc");
        product.setPrice(price);
        setId(product, id);
        return product;
    }

    private void setId(Product product, UUID id) {
        try {
            var field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_savesEntityBuiltFromRequestAndReturnsResponse() {
        var request = new ProductRequest("Widget", "a widget", "SKU-1", BigDecimal.TEN);
        UUID id = UUID.randomUUID();
        Product saved = product(id, "SKU-1", "Widget", BigDecimal.TEN);
        when(repository.save(any(Product.class))).thenReturn(saved);

        var response = service.create(request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.name()).isEqualTo("Widget");
    }

    @Test
    void getById_returnsMappedResponseWhenFound() {
        UUID id = UUID.randomUUID();
        Product existing = product(id, "SKU-2", "Gadget", BigDecimal.ONE);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        var response = service.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.sku()).isEqualTo("SKU-2");
    }

    @Test
    void getById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        var ex = assertThrows(ProductNotFoundException.class, () -> service.getById(id));

        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    void listAll_mapsEachPageEntryToResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Product p1 = product(UUID.randomUUID(), "SKU-3", "A", BigDecimal.ONE);
        Product p2 = product(UUID.randomUUID(), "SKU-4", "B", BigDecimal.TEN);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

        var page = service.listAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting("sku").containsExactly("SKU-3", "SKU-4");
    }

    @Test
    void update_mutatesExistingEntityInPlaceWithoutExplicitSave() {
        UUID id = UUID.randomUUID();
        Product existing = product(id, "SKU-5", "Old name", BigDecimal.ONE);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        var request = new ProductRequest("New name", "new desc", "SKU-5-NEW", BigDecimal.valueOf(99));

        var response = service.update(id, request);

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.sku()).isEqualTo("SKU-5-NEW");
        assertThat(existing.getName()).isEqualTo("New name");
        verify(repository, never()).save(any());
    }

    @Test
    void update_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        var request = new ProductRequest("New name", null, "SKU-6", BigDecimal.ONE);

        assertThrows(ProductNotFoundException.class, () -> service.update(id, request));
    }

    @Test
    void delete_removesEntityWhenFound() {
        UUID id = UUID.randomUUID();
        Product existing = product(id, "SKU-7", "To delete", BigDecimal.ONE);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(repository).delete(existing);
    }

    @Test
    void delete_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.delete(id));
    }
}
