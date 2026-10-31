package com.tuantha8203.shop.customer.internal;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CustomerWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<CustomerIdentities> identities;

    CustomerWebConfig(ObjectProvider<CustomerIdentities> identities) {
        this.identities = identities;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentCustomerIdResolver(identities));
    }
}
