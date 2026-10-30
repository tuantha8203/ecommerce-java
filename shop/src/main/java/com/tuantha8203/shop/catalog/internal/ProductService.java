package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.catalog.CatalogApi;
import com.tuantha8203.shop.catalog.ProductSummary;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // default for every method; write methods override it
public class ProductService implements CatalogApi {

    private final ProductRepository products;
    private final CategoryRepository categories;

    @Transactional
    public ProductDetails create(CreateProductRequest request) {
        Category category = categories
                .findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        // Friendly early check. Two concurrent requests can still both pass it,
        // so the UNIQUE constraint in the database remains the real guard (handled in step 1.5).
        if (products.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = new Product(
                category, request.sku(), request.name(), request.slug(), request.description(), request.price());
        return ProductDetails.from(products.save(product));
    }

    public ProductDetails getBySlug(String slug) {
        return products.findBySlug(slug)
                .map(ProductDetails::from)
                .orElseThrow(() -> new ProductNotFoundException(slug));
    }

    public ProductDetails getById(long id) {
        return ProductDetails.from(load(id));
    }

    @Transactional
    public ProductDetails update(long id, UpdateProductRequest request) {
        Product product = load(id);
        product.updateDetails(request.name(), request.description(), request.price());
        products.flush(); // write now so @Version is incremented before we read it into the DTO
        return ProductDetails.from(product);
    }

    @Transactional
    public void activate(long id) {
        load(id).activate();
    }

    @Transactional
    public void archive(long id) {
        load(id).archive();
    }

    @Override
    public List<ProductSummary> findProductsByIds(Collection<Long> ids) {
        return products.findAllById(ids).stream().map(ProductService::toSummary).toList();
    }

    private Product load(long id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(String.valueOf(id)));
    }

    private static ProductSummary toSummary(Product product) {
        return new ProductSummary(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                product.getStatus());
    }
}
