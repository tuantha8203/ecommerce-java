package tuantha8203.inventoryservice.application;

import java.util.UUID;

public class LockAcquisitionFailedException extends RuntimeException {
    public LockAcquisitionFailedException(UUID productId) {
        super("Could not acquire lock for product " + productId);
    }
}
