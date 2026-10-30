package com.tuantha8203.shop.payment.internal;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WebhookSignatures {

    private final PaymentProperties properties;

    boolean isValid(String body, String signature) {
        if (signature == null) {
            return false;
        }
        // MessageDigest.isEqual takes the same time whatever the input, so an attacker
        // cannot guess the signature byte by byte by measuring response times
        return MessageDigest.isEqual(
                sign(body).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot compute webhook signature", ex);
        }
    }
}
