package com.tuantha8203.shop.inventory.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ecommerce-java.inventory")
public record InventoryProperties(
        @DefaultValue("PT15M") Duration reservationTtl,
        @DefaultValue("PT30S") Duration expiryCheckInterval,
        @DefaultValue("100") int expiryBatchSize) {}
