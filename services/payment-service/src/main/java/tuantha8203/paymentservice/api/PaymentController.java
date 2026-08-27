package tuantha8203.paymentservice.api;

import tuantha8203.paymentservice.application.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/charge")
    public PaymentResponse charge(@RequestBody PaymentRequest request) {
        return service.charge(request);
    }
}
