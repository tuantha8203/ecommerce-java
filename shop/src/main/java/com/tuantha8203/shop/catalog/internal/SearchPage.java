package com.tuantha8203.shop.catalog.internal;

import java.util.List;

/** One page of search results. hasMore tells whether a next page exists, without a costly count(*). */
public record SearchPage(List<ProductCard> items, int page, int size, boolean hasMore) {}
