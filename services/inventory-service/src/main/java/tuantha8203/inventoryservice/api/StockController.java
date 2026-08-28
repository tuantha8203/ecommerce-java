package tuantha8203.inventoryservice.api;

import jakarta.validation.Valid;
import tuantha8203.common.api.ApiResponse;
import tuantha8203.inventoryservice.application.StockNotFoundException;
import tuantha8203.inventoryservice.application.StockService;
import tuantha8203.inventoryservice.domain.Stock;
import tuantha8203.inventoryservice.infrastructure.StockRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class StockController {

    private final StockRepository repository;
    private final StockService stockService;

    public StockController(StockRepository repository, StockService stockService) {
        this.repository = repository;
        this.stockService = stockService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<StockResponse>> getStock(@PathVariable UUID productId) {
        Stock stock = repository.findById(productId).orElseThrow(() -> new StockNotFoundException(productId));
        return ResponseEntity.ok(ApiResponse.success(StockResponse.from(stock)));
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<ApiResponse<StockResponse>> restock(@PathVariable UUID productId,
                                                                @Valid @RequestBody RestockRequest request) {
        boolean ok = stockService.restock(productId, request.quantity());
        if (!ok) {
            throw new StockNotFoundException(productId);
        }
        Stock stock = repository.findById(productId).orElseThrow(() -> new StockNotFoundException(productId));
        return ResponseEntity.ok(ApiResponse.success(StockResponse.from(stock)));
    }
}