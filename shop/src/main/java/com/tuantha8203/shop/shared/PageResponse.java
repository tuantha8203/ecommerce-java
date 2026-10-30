package com.tuantha8203.shop.shared;

import java.util.List;
import org.springframework.data.domain.Page;

/** Offset pagination result. Our own DTO, so the JSON shape does not depend on Spring Data internals. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
