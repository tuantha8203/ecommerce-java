package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.shared.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/** Position of the last item of a page: (createdAt, id). Encoded as an opaque string for clients. */
record ProductCursor(Instant createdAt, long id) {

    String encode() {
        String raw = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static ProductCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            return new ProductCursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException ex) {
            throw new InvalidRequestException("Invalid cursor");
        }
    }
}
