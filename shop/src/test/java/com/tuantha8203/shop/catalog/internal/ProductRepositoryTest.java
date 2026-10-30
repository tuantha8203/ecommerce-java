package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.catalog.ProductStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    CategoryRepository categories;

    @Autowired
    ProductRepository products;

    @Autowired
    EntityManager em;

    @Test
    void savesAndReloadsProductWithImages() {
        Category shirts = categories.save(new Category("Shirts", "shirts", null));
        Product product = new Product(shirts, "SKU-1", "T-shirt", "t-shirt", null, 150_000);
        product.addImage("https://cdn.example.com/1.jpg", "front view");
        products.save(product);

        em.flush();
        em.clear();

        Product found = products.findBySlug("t-shirt").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(found.getImages()).extracting(ProductImage::getUrl).containsExactly("https://cdn.example.com/1.jpg");
    }

    @Test
    void rejectsDuplicateSkuAtDatabaseLevel() {
        Category shirts = categories.save(new Category("Shirts", "shirts", null));
        products.saveAndFlush(new Product(shirts, "SKU-1", "Shirt A", "shirt-a", null, 100_000));

        assertThatThrownBy(
                        () -> products.saveAndFlush(new Product(shirts, "SKU-1", "Shirt B", "shirt-b", null, 100_000)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativePriceInEntity() {
        Category shirts = categories.save(new Category("Shirts", "shirts", null));

        assertThatThrownBy(() -> new Product(shirts, "SKU-2", "Shirt C", "shirt-c", null, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
