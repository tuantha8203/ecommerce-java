package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.catalog.ProductStatus;
import java.util.List;

public record ProductDetails(
        Long id,
        String sku,
        String name,
        String slug,
        String description,
        long price,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        List<Image> images,
        long version) {

    public record Image(String url, String altText) {}

    /** Must be called inside a transaction: it touches the lazy category and images. */
    static ProductDetails from(Product product) {
        List<Image> images = product.getImages().stream()
                .map(image -> new Image(image.getUrl(), image.getAltText()))
                .toList();
        return new ProductDetails(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                images,
                product.getVersion());
    }
}
