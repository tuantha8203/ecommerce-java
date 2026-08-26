package tuantha8203.inventoryservice.api;

import tuantha8203.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

public enum InventoryErrorCode implements ErrorCode {
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND);

    private final HttpStatus status;

    InventoryErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
