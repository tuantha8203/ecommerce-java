package tuantha8203.inventoryservice.application;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(UUID productId) {
        super("Stock not found for product ID: " + productId);
    }
}
