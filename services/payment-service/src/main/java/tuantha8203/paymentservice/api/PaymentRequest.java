package tuantha8203.paymentservice.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID orderId, BigDecimal amount) {}
