package tuantha8203.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("payment")
public record PaymentProperties(@DefaultValue("0.2") double failureRate) {}
