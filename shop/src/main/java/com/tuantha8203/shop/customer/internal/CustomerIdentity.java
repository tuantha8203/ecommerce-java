package com.tuantha8203.shop.customer.internal;

/** Who the access token says the caller is. */
public record CustomerIdentity(String subject, String email, String name, boolean emailVerified) {

    String displayName() {
        return name == null || name.isBlank() ? email : name;
    }
}
