package tuantha8203.paymentservice.application;

import tuantha8203.paymentservice.api.PaymentRequest;
import tuantha8203.paymentservice.api.PaymentResponse;
import tuantha8203.paymentservice.config.PaymentProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private final PaymentProperties properties;

    public PaymentService(PaymentProperties properties) {
        this.properties = properties;
    }

    public PaymentResponse charge(PaymentRequest request) {
        boolean success = ThreadLocalRandom.current().nextDouble() >= properties.failureRate();
        // TODO Phase 3.3/12.1: lưu Payment entity + idempotency_key thay vì chỉ trả kết quả tạm ở đây
        return new PaymentResponse(success);
    }
}
