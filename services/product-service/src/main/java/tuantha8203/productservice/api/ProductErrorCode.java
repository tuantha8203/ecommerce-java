package tuantha8203.productservice.api;

import tuantha8203.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    SKU_ALREADY_EXISTS(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ProductErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
