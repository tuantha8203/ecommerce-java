package com.tuantha8203.shop.shared;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Fixed-window rate limiting, counted in Redis so every app instance shares the same counters. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    // INCR and EXPIRE in ONE atomic step: if the app died between two separate commands,
    // the key would never expire and the customer would be locked out forever
    private static final RedisScript<Long> INCREMENT_WITH_EXPIRY = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    /** Throws TooManyRequestsException once `limit` calls for this subject happened in the current window. */
    public void check(String action, String subject, int limit, Duration window) {
        long windowSeconds = window.toSeconds();
        long now = clock.instant().getEpochSecond();
        long windowStart = now - (now % windowSeconds);
        String key = "rate:" + action + ":" + subject + ":" + windowStart;

        Long count;
        try {
            count = redis.execute(INCREMENT_WITH_EXPIRY, List.of(key), String.valueOf(windowSeconds));
        } catch (DataAccessException ex) {
            // Fail OPEN: a broken rate limiter must not stop customers from buying (see ADR)
            log.warn("Rate limiter unavailable, letting the request through: {}", ex.getMessage());
            return;
        }
        if (count != null && count > limit) {
            throw new TooManyRequestsException(Duration.ofSeconds(windowStart + windowSeconds - now));
        }
    }
}
