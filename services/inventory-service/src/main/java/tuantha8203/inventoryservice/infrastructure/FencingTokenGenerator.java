package tuantha8203.inventoryservice.infrastructure;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FencingTokenGenerator {

    private static final String FENCING_TOKEN_KEY = "fencing:token:";

    private final StringRedisTemplate redis;

    public FencingTokenGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long next(UUID productId) {
        return redis.opsForValue().increment(FENCING_TOKEN_KEY + productId);
    }
}