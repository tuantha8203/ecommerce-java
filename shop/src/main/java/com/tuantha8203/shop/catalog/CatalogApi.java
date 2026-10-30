package com.tuantha8203.shop.catalog;

import java.util.Collection;
import java.util.List;

/** The only entry point other modules may use to read catalog data. */
public interface CatalogApi {

    /** Batch lookup: one call for many ids, so this stays cheap when it becomes an HTTP call later. */
    List<ProductSummary> findProductsByIds(Collection<Long> ids);
}
