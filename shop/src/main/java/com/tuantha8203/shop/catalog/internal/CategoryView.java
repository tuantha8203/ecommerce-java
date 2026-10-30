package com.tuantha8203.shop.catalog.internal;

public record CategoryView(Long id, String name, String slug, Long parentId) {

    static CategoryView from(Category category) {
        Long parentId =
                category.getParent() == null ? null : category.getParent().getId();
        return new CategoryView(category.getId(), category.getName(), category.getSlug(), parentId);
    }
}
