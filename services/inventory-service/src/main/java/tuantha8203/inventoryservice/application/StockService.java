package tuantha8203.inventoryservice.application;

import tuantha8203.inventoryservice.infrastructure.FencingTokenGenerator;
import tuantha8203.inventoryservice.infrastructure.RedisLockService;
import tuantha8203.inventoryservice.infrastructure.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional
public class StockService {

    private final StockRepository repository;
    private final RedisLockService lockService;
    private final FencingTokenGenerator tokenGenerator;

    private static final String LOCK_STOCK_KEY = "lock:stock:";

    public StockService(StockRepository repository, RedisLockService lockService,
                         FencingTokenGenerator tokenGenerator) {
        this.repository = repository;
        this.lockService = lockService;
        this.tokenGenerator = tokenGenerator;
    }

    public boolean reserve(UUID productId, int qty) {
        String lockKey = LOCK_STOCK_KEY + productId;
        String lockToken = lockService.tryLock(lockKey, Duration.ofSeconds(5));
        if (lockToken == null) {
            throw new LockAcquisitionFailedException(productId);
        }
        try {
            long fencingToken = tokenGenerator.next(productId);
            int updated = repository.reserveIfAvailable(productId, qty, fencingToken);
            return updated > 0;
        } finally {
            lockService.unlock(lockKey, lockToken);
        }
    }

    public boolean release(UUID productId, int qty) {
        String lockKey = LOCK_STOCK_KEY + productId;
        String lockToken = lockService.tryLock(lockKey, Duration.ofSeconds(5));
        if (lockToken == null) {
            throw new LockAcquisitionFailedException(productId);
        }
        try {
            long fencingToken = tokenGenerator.next(productId);
            int updated = repository.releaseIfNewerToken(productId, qty, fencingToken);
            return updated > 0;
        } finally {
            lockService.unlock(lockKey, lockToken);
        }
    }

    // Nhập kho — nghiệp vụ ops (thêm hàng mới về), không phải một bước trong saga đặt hàng như
    // reserve()/release(). Vẫn đi qua cùng Redis lock + fencing token để không đá nhau nếu chạy đồng
    // thời với 1 order đang reserve/release đúng lúc admin restock.
    public boolean restock(UUID productId, int qty) {
        String lockKey = LOCK_STOCK_KEY + productId;
        String lockToken = lockService.tryLock(lockKey, Duration.ofSeconds(5));
        if (lockToken == null) {
            throw new LockAcquisitionFailedException(productId);
        }
        try {
            long fencingToken = tokenGenerator.next(productId);
            int updated = repository.restock(productId, qty, fencingToken);
            return updated > 0;
        } finally {
            lockService.unlock(lockKey, lockToken);
        }
    }
}