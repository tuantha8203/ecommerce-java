package tuantha8203.productservice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

public record PaginationResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        @JsonProperty("isFirst") boolean isFirst,
        @JsonProperty("isLast") boolean isLast
) {
    public static PaginationResponse from(Page<?> page) {
        return new PaginationResponse(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious(),
            page.isFirst(),
            page.isLast()
        );
    }
}