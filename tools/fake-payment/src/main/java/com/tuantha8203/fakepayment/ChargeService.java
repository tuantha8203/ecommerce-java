package com.tuantha8203.fakepayment;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChargeService {

    record IdempotencyEntry(String fingerprint, String chargeId) {}

    public record ChargeResult(Charge charge, boolean created) {}

    private final Map<String, Charge> charges = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyEntry> idempotencyKeys = new ConcurrentHashMap<>();
    private final Chaos chaos;
    private final WebhookSender webhooks;

    public ChargeResult charge(String idempotencyKey, ChargeRequest request) {
        AtomicBoolean created = new AtomicBoolean(false);

        // computeIfAbsent is atomic per key: two concurrent requests with the same key
        // can never both create a charge
        IdempotencyEntry entry = idempotencyKeys.computeIfAbsent(idempotencyKey, key -> {
            Charge charge = newCharge(request);
            charges.put(charge.id(), charge);
            created.set(true);
            return new IdempotencyEntry(request.fingerprint(), charge.id());
        });

        if (!entry.fingerprint().equals(request.fingerprint())) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422), "Idempotency-Key was already used for a different request");
        }

        Charge charge = charges.get(entry.chargeId());
        if (created.get()) {
            webhooks.send(charge);
        }
        return new ChargeResult(charge, created.get());
    }

    public Optional<Charge> find(String chargeId) {
        return Optional.ofNullable(charges.get(chargeId));
    }

    /** Lets the shop ask "did anything happen for order X?" when it never got a response. */
    public List<Charge> findByReference(String reference) {
        return charges.values().stream()
                .filter(charge -> charge.reference().equals(reference))
                .sorted(Comparator.comparing(Charge::createdAt))
                .toList();
    }

    /** Refunding is naturally idempotent: a refunded charge stays refunded. */
    public Charge refund(String chargeId) {
        Charge before = find(chargeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge not found"));
        if (before.status() == ChargeStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A declined charge cannot be refunded");
        }
        Charge after = charges.computeIfPresent(chargeId, (id, charge) -> charge.withStatus(ChargeStatus.REFUNDED));
        if (before.status() == ChargeStatus.SUCCEEDED) {
            webhooks.send(after);
        }
        return after;
    }

    private Charge newCharge(ChargeRequest request) {
        boolean declined = "tok_declined".equals(request.cardToken())
                || chaos.happens(chaos.settings().declineRate());
        return new Charge(
                "ch_" + UUID.randomUUID(),
                request.reference(),
                request.amount(),
                request.currency(),
                declined ? ChargeStatus.DECLINED : ChargeStatus.SUCCEEDED,
                declined ? "card_declined" : null,
                Instant.now());
    }
}