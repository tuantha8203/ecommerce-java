package com.tuantha8203.shop.payment.internal;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestClient;

@Configuration
@EnableResilientMethods // turns on @Retryable
@EnableConfigurationProperties(PaymentProperties.class)
class PaymentConfig {

    @Bean
    RestClient paymentGatewayRestClient(PaymentProperties properties) {
        // Never call a remote system without timeouts: the default is to wait forever
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.gatewayUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
