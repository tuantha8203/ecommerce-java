package com.tuantha8203.shop.payment.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ecommerce-java.payment")
public record PaymentProperties(
        String gatewayUrl,
        String webhookSecret,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("PT1M") Duration reconcileAfter,
        @DefaultValue("PT10M") Duration giveUpAfter) {}
