package tuantha8203.productservice.application;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import tuantha8203.productservice.api.ProductRequest;
import tuantha8203.productservice.domain.Product;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toEntity_copiesAllFieldsFromRequest() {
        var request = new ProductRequest("Widget", "a widget", "SKU-1", BigDecimal.TEN);

        Product entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("Widget");
        assertThat(entity.getDescription()).isEqualTo("a widget");
        assertThat(entity.getSku()).isEqualTo("SKU-1");
        assertThat(entity.getPrice()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void toResponse_copiesAllFieldsFromEntity() {
        Product entity = mapper.toEntity(new ProductRequest("Widget", "a widget", "SKU-1", BigDecimal.TEN));

        var response = mapper.toResponse(entity);

        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.description()).isEqualTo("a widget");
        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.price()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void updateEntity_overwritesAllMutableFieldsIncludingSku() {
        Product entity = mapper.toEntity(new ProductRequest("Old", "old desc", "SKU-OLD", BigDecimal.ONE));

        mapper.updateEntity(entity, new ProductRequest("New", "new desc", "SKU-NEW", BigDecimal.TEN));

        assertThat(entity.getName()).isEqualTo("New");
        assertThat(entity.getDescription()).isEqualTo("new desc");
        assertThat(entity.getSku()).isEqualTo("SKU-NEW");
        assertThat(entity.getPrice()).isEqualTo(BigDecimal.TEN);
    }
}
