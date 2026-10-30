package com.tuantha8203.fakepayment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fake-payment")
public record FakePaymentProperties(String webhookUrl, String webhookSecret, ChaosSettings chaos) {}