package com.tuantha8203.shop.customer.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddAddressRequest(
        @NotBlank @Size(max = 150) String receiverName,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{9,15}") String phone,
        @NotBlank @Size(max = 255) String street,
        @NotBlank @Size(max = 100) String ward,
        @NotBlank @Size(max = 100) String province) {}
