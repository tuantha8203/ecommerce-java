package tuantha8203.orderservice.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private static final String CHARGE_URL = "http://payment-service/api/v1/payments/charge";

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean charge(UUID orderId, BigDecimal amount) {
        var request = new ChargeRequest(orderId, amount);
        try {
            var response = restTemplate.postForEntity(CHARGE_URL, request, ChargeResponse.class);
            return response.getBody() != null && response.getBody().success();
        } catch (ResourceAccessException e) {
            // timeout/connection refused — lỗi hạ tầng tạm thời, khác với payment-service trả lỗi nghiệp vụ
            log.error("Payment charge timeout/connection error for order {}", orderId, e);
            return false;
        } catch (RestClientException e) {
            log.error("Payment charge call failed for order {}", orderId, e);
            return false;
        }
    }

    record ChargeRequest(UUID orderId, BigDecimal amount) {}
    record ChargeResponse(boolean success) {}
}