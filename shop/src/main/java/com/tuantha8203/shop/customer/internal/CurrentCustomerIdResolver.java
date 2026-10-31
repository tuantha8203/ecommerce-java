package com.tuantha8203.shop.customer.internal;

import com.tuantha8203.shop.customer.CurrentCustomerId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class CurrentCustomerIdResolver implements HandlerMethodArgumentResolver {

    // ObjectProvider: looked up lazily, so slice tests (@WebMvcTest) start without the customer services
    private final ObjectProvider<CustomerIdentities> identities;

    CurrentCustomerIdResolver(ObjectProvider<CustomerIdentities> identities) {
        this.identities = identities;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentCustomerId.class)
                && (parameter.getParameterType() == long.class || parameter.getParameterType() == Long.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("@CurrentCustomerId needs a request authenticated with a JWT");
        }
        Jwt jwt = token.getToken();
        return identities
                .getObject()
                .resolve(new CustomerIdentity(
                        jwt.getSubject(),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("name"),
                        Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))));
    }
}
