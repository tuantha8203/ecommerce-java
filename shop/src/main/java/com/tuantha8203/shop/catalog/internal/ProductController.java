package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.CursorPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
    ResponseEntity<ProductDetails> getBySlug(@PathVariable String slug) {
        ProductDetails product = products.getPublishedBySlug(slug);
        return ResponseEntity.ok()
                .eTag(String.valueOf(product.version()))
                // public: browsers AND shared caches (CDN) may keep it for 60 seconds
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(product);
    }

    @GetMapping
    CursorPage<ProductCard> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return products.listPublished(category, after, limit);
    }
}
