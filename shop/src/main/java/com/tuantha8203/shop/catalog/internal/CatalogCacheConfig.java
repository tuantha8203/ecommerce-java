package com.tuantha8203.shop.catalog.internal;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
class CatalogCacheConfig implements CachingConfigurer {

    static final String PRODUCT_PAGES = "productPages";

    /** Redis unavailable: log it and run the method (read the database) instead of failing the request. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    @Bean
    RedisCacheManagerBuilderCustomizer catalogCaches() {
        return builder -> builder
                // puts and evictions wait for the surrounding transaction to commit:
                // evicting before commit would let a concurrent reader re-cache the OLD row
                .transactionAware()
                .withCacheConfiguration(
                        PRODUCT_PAGES,
                        RedisCacheConfiguration.defaultCacheConfig()
                                // 10 minutes plus up to 60 random seconds: keys written together
                                // do not all expire in the same instant and hit the database at once
                                .entryTtl((key, value) -> Duration.ofMinutes(10)
                                        .plusSeconds(ThreadLocalRandom.current().nextInt(60)))
                                // typed JSON: readable in redis-cli, and no class names stored in the payload
                                .serializeValuesWith(SerializationPair.fromSerializer(
                                        new JacksonJsonRedisSerializer<>(ProductDetails.class)))
                                .disableCachingNullValues());
    }
}
