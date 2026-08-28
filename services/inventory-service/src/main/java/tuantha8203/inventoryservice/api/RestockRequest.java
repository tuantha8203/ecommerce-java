package tuantha8203.inventoryservice.api;

import jakarta.validation.constraints.Positive;

public record RestockRequest(@Positive int quantity) {}
