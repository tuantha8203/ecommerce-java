package com.tuantha8203.shop.catalog.internal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "catalog", name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 500)
    private String url;

    private String altText;

    @Column(nullable = false)
    private int position;

    // Không public: ảnh chỉ được tạo qua Product.addImage
    ProductImage(Product product, String url, String altText, int position) {
        this.product = product;
        this.url = url;
        this.altText = altText;
        this.position = position;
    }
}
