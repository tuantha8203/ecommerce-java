package tuantha8203.common.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
    boolean success,
    String errorCode,
    String message,
    T data,
    PaginationResponse pagination,
    List<FieldError> errors,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, "OK", data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> successPaged(T data, PaginationResponse pagination) {
        return new ApiResponse<>(true, null, "OK", data, pagination, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.name(), message, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> validationError(List<FieldError> errors) {
        return new ApiResponse<>(false, CommonErrorCode.VALIDATION_FAILED.name(), "Validation failed", null, null,
                errors, Instant.now());
    }
}
