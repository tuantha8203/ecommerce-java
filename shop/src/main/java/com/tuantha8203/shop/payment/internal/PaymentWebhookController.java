package com.tuantha8203.shop.payment.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
class PaymentWebhookController {

    private final WebhookSignatures signatures;
    private final PaymentService payments;
    private final JsonMapper jsonMapper;

    // The body arrives as a raw String: the signature covers the exact bytes sent,
    // and parsing first then re-serializing could change them
    @PostMapping
    ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Fake-Payment-Signature", required = false) String signature,
            @RequestBody String body) {
        if (!signatures.isValid(body, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        payments.handleWebhook(jsonMapper.readValue(body, WebhookEvent.class));
        return ResponseEntity.ok().build(); // also 200 for duplicates: otherwise the gateway keeps resending
    }
}
