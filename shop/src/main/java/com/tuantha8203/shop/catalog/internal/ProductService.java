package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.catalog.CatalogApi;
import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.catalog.ProductSummary;
import com.tuantha8203.shop.shared.ConstraintViolations;
import com.tuantha8203.shop.shared.CursorPage;
import com.tuantha8203.shop.shared.PageResponse;
import com.tuantha8203.shop.shared.PreconditionFailedException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // default for every method; write methods override it
public class ProductService implements CatalogApi {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductListingQueries listingQueries;
    private final CacheManager cacheManager;

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
        try {
            return ProductDetails.from(products.saveAndFlush(product));
        } catch (DataIntegrityViolationException ex) {
            throw translate(ex, request);
        }
    }

    /** Turns a known constraint violation into a business error; anything else stays as it is. */
    private static RuntimeException translate(DataIntegrityViolationException ex, CreateProductRequest request) {
        return switch (ConstraintViolations.constraintName(ex).orElse("")) {
            case "uq_products_sku" -> new DuplicateSkuException(request.sku());
            case "uq_products_slug" -> new DuplicateProductSlugException(request.slug());
            default -> ex;
        };
    }

    public ProductDetails getById(long id) {
        return ProductDetails.from(load(id));
    }

    @Cacheable(cacheNames = CatalogCacheConfig.PRODUCT_PAGES, key = "#slug", sync = true)
    public ProductDetails getPublishedBySlug(String slug) {
        return products.findBySlug(slug)
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .map(ProductDetails::from)
                .orElseThrow(() -> new ProductNotFoundException(slug)); // drafts look exactly like missing products
    }

    @Transactional
    public ProductDetails update(long id, long expectedVersion, UpdateProductRequest request) {
        Product product = load(id);
        if (product.getVersion() != expectedVersion) {
            throw new PreconditionFailedException(
                    "Product " + id + " was modified (current version " + product.getVersion() + ")");
        }
        product.updateDetails(request.name(), request.description(), request.price());
        products.flush(); // write now so @Version is incremented before we read it into the DTO
        evictProductPage(product.getSlug());
        return ProductDetails.from(product);
    }

    /** Public listing with cursor pagination. */
    public CursorPage<ProductCard> listPublished(String categorySlug, String afterToken, int limit) {
        ProductCursor after = afterToken == null ? null : ProductCursor.decode(afterToken);

        // Ask for one extra row: if it exists, there is a next page
        List<ProductListingQueries.Row> rows = listingQueries.findPublished(categorySlug, after, limit + 1);
        boolean hasMore = rows.size() > limit;
        List<ProductListingQueries.Row> page = hasMore ? rows.subList(0, limit) : rows;

        String nextCursor = hasMore ? page.getLast().position().encode() : null;
        return new CursorPage<>(
                page.stream().map(ProductListingQueries.Row::card).toList(), nextCursor);
    }

    /** Admin listing with offset pagination: random access to any page, plus a total count. */
    public PageResponse<ProductSummary> listForAdmin(ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")));
        Page<Product> result = status == null ? products.findAll(pageable) : products.findByStatus(status, pageable);
        return PageResponse.from(result.map(ProductService::toSummary));
    }

    /** With transactionAware(), the eviction actually happens right after the transaction commits. */
    private void evictProductPage(String slug) {
        Cache cache = cacheManager.getCache(CatalogCacheConfig.PRODUCT_PAGES);
        if (cache != null) {
            cache.evict(slug);
        }
    }

    @Transactional
    public void activate(long id) {
        Product product = load(id);
        product.activate();
        evictProductPage(product.getSlug());
    }

    @Transactional
    public void archive(long id) {
        Product product = load(id);
        product.archive();
        evictProductPage(product.getSlug()); // the page must disappear, not linger for 10 minutes
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
