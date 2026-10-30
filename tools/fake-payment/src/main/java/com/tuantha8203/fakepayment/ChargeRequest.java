package com.tuantha8203.fakepayment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * cardToken works like test cards of real gateways:
 * "tok_declined" is always declined, anything else succeeds unless chaos says otherwise.
 */
public record ChargeRequest(
        @NotBlank @Size(max = 64) String reference,
        @Positive long amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        String cardToken) {

    /** Same key + different fingerprint means the client reused a key for another payment. */
    String fingerprint() {
        return reference + "|" + amount + "|" + currency + "|" + cardToken;
    }
}