package tuantha8203.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("kafka.dlq")
public record KafkaDlqProperties(
        @DefaultValue("1000") long backoffMs,
        @DefaultValue("3") int maxAttempts
) {}
