package com.tuantha8203.shop.catalog;

/** Minimal product data other modules are allowed to see. */
public record ProductSummary(Long id, String sku, String name, String slug, long price, ProductStatus status) {}
