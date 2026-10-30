package com.tuantha8203.shop.customer.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "\\+?[0-9]{9,15}") String phone) {}
