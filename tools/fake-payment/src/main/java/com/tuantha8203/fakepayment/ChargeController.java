package com.tuantha8203.fakepayment;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/charges")
@RequiredArgsConstructor
class ChargeController {

    private final ChargeService charges;
    private final Chaos chaos;

    @PostMapping
    ResponseEntity<Charge> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody ChargeRequest request) {
        ChaosSettings settings = chaos.settings();
        chaos.simulateLatency();

        // Failure BEFORE processing: nothing happened, retrying is safe
        if (chaos.happens(settings.failureRate())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated failure before processing");
        }

        ChargeService.ChargeResult result = charges.charge(idempotencyKey, request);

        // Failure AFTER processing: the money moved, but the client will time out before hearing about it
        if (result.created() && chaos.happens(settings.lostResponseRate())) {
            chaos.sleep(Duration.ofMillis(settings.lostResponseDelayMs()));
        }

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK; // 200 = replay of an earlier request
        return ResponseEntity.status(status).body(result.charge());
    }

    @GetMapping("/{chargeId}")
    Charge get(@PathVariable String chargeId) {
        return charges.find(chargeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge not found"));
    }

    @GetMapping
    List<Charge> findByReference(@RequestParam String reference) {
        return charges.findByReference(reference);
    }

    @PostMapping("/{chargeId}/refund")
    Charge refund(@PathVariable String chargeId) {
        return charges.refund(chargeId);
    }
}