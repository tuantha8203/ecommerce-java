package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.catalog.ProductStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "catalog", name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Version
    private long version;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<ProductImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public Product(Category category, String sku, String name, String slug, String description, long price) {
        requireNonNegative(price);
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.price = price;
        this.status = ProductStatus.DRAFT;
    }

    public void changePrice(long newPrice) {
        requireNonNegative(newPrice);
        this.price = newPrice;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void archive() {
        this.status = ProductStatus.ARCHIVED;
    }

    public void addImage(String url, String altText) {
        images.add(new ProductImage(this, url, altText, images.size()));
    }

    private static void requireNonNegative(long price) {
        if (price < 0) {
            throw new IllegalArgumentException("Giá không được âm");
        }
    }

    public void updateDetails(String name, String description, long price) {
        requireNonNegative(price);
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
