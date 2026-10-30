package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.CursorPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
class ProductController {

    private final ProductService products;

    @GetMapping("/{slug}")
    ProductDetails getBySlug(@PathVariable String slug) {
        return products.getPublishedBySlug(slug);
    }

    @GetMapping
    CursorPage<ProductCard> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return products.listPublished(category, after, limit);
    }
}
