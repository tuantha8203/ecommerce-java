package com.tuantha8203.fakepayment;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Holds the current failure settings; they can be changed at runtime through /admin/chaos. */
@Component
public class Chaos {

    private final AtomicReference<ChaosSettings> settings;

    public Chaos(FakePaymentProperties properties) {
        this.settings = new AtomicReference<>(properties.chaos());
    }

    public ChaosSettings settings() {
        return settings.get();
    }

    public void update(ChaosSettings newSettings) {
        settings.set(newSettings);
    }

    /** True with the given probability, e.g. 0.3 means about 30% of calls. */
    public boolean happens(double rate) {
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    public void simulateLatency() {
        ChaosSettings current = settings();
        long min = current.minLatencyMs();
        long max = Math.max(min, current.maxLatencyMs());
        sleep(Duration.ofMillis(ThreadLocalRandom.current().nextLong(min, max + 1)));
    }

    public void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}