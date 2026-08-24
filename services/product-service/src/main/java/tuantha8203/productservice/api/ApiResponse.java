package tuantha8203.productservice.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
    boolean success,
    int code,
    String message,
    T data,
    PaginationResponse pagination,
    List<FieldError> errors,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "OK", data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> successPaged(T data, PaginationResponse pagination) {
        return new ApiResponse<>(true, 200, "OK", data, pagination, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> validationError(List<FieldError> errors) {
        return new ApiResponse<>(false, 400, "Validation failed", null, null, errors, Instant.now());
    }
}