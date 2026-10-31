package com.tuantha8203.shop;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.tuantha8203.shop.customer.internal.CustomerIdentity;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Fake logged-in users for MockMvc. No Keycloak needed: the token is created already validated. */
public final class TestAuth {

    private TestAuth() {}

    public static String newSubject() {
        return "user-" + UUID.randomUUID();
    }

    /** The same identity the resolver builds from customer(subject); use it to set up data via services. */
    public static CustomerIdentity identity(String subject) {
        return new CustomerIdentity(subject, subject + "@test.local", "Test " + subject, true);
    }

    public static RequestPostProcessor customer(String subject) {
        return jwt().jwt(token -> token.subject(subject)
                        .claim("email", subject + "@test.local")
                        .claim("email_verified", true)
                        .claim("name", "Test " + subject))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    public static RequestPostProcessor admin() {
        String subject = "admin-" + UUID.randomUUID();
        return jwt().jwt(token -> token.subject(subject)
                        .claim("email", subject + "@test.local")
                        .claim("email_verified", true))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
