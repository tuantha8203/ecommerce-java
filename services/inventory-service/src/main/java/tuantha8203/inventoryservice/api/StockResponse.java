package tuantha8203.inventoryservice.api;

import tuantha8203.inventoryservice.domain.Stock;

import java.util.UUID;

public record StockResponse(
        UUID productId,
        int availableQty,
        int reservedQty,
        long lastFencingToken
) {
    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getProductId(),
                stock.getAvailableQty(),
                stock.getReservedQty(),
                stock.getLastFencingToken()
        );
    }
}
