package tuantha8203.inventoryservice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tuantha8203.common.api.ApiResponse;
import tuantha8203.common.api.CommonErrorCode;
import tuantha8203.common.api.ErrorCode;
import tuantha8203.inventoryservice.application.StockNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(StockNotFoundException ex) {
        ErrorCode errorCode = InventoryErrorCode.STOCK_NOT_FOUND;
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.error(errorCode, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.error(errorCode, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.error(errorCode, "Internal server error"));
    }
}
