package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuantha8203.shop.TestcontainersConfiguration;
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
    void luuVaDocLaiSanPhamKemAnh() {
        Category ao = categories.save(new Category("Áo", "ao", null));
        Product p = new Product(ao, "SKU-1", "Áo thun", "ao-thun", null, 150_000);
        p.addImage("https://cdn.example.com/1.jpg", "mặt trước");
        products.save(p);

        em.flush();
        em.clear();

        Product found = products.findBySlug("ao-thun").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(found.getImages()).extracting(ProductImage::getUrl).containsExactly("https://cdn.example.com/1.jpg");
    }

    @Test
    void trungSkuBiDatabaseTuChoi() {
        Category ao = categories.save(new Category("Áo", "ao", null));
        products.saveAndFlush(new Product(ao, "SKU-1", "Áo A", "ao-a", null, 100_000));

        assertThatThrownBy(() -> products.saveAndFlush(new Product(ao, "SKU-1", "Áo B", "ao-b", null, 100_000)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void giaAmBiTuChoiNgayTrongEntity() {
        Category ao = categories.save(new Category("Áo", "ao", null));

        assertThatThrownBy(() -> new Product(ao, "SKU-2", "Áo C", "ao-c", null, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
