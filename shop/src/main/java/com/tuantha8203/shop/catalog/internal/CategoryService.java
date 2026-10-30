package com.tuantha8203.shop.catalog.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;

    public List<CategoryView> findAll() {
        return categories.findAll(Sort.by("name")).stream()
                .map(CategoryView::from)
                .toList();
    }

    @Transactional
    public CategoryView create(CreateCategoryRequest request) {
        Category parent = request.parentId() == null
                ? null
                : categories
                        .findById(request.parentId())
                        .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
        Category saved = categories.save(new Category(request.name(), request.slug(), parent));
        return CategoryView.from(saved);
    }
}
