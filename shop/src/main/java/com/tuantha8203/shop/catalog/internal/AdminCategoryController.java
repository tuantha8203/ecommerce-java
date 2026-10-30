package com.tuantha8203.shop.catalog.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
class AdminCategoryController {

    private final CategoryService categories;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryView create(@Valid @RequestBody CreateCategoryRequest request) {
        return categories.create(request);
    }
}
