package com.tuantha8203.shop.shared;

/** Temporary caller identity until phase 3 replaces it with the JWT subject. */
public final class CallerHeaders {

    public static final String CUSTOMER_ID = "X-Customer-Id";

    private CallerHeaders() {}
}
