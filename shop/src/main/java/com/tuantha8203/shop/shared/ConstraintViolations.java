package com.tuantha8203.shop.shared;

import java.util.Locale;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;

/** Reads the name of the database constraint behind a DataIntegrityViolationException. */
public final class ConstraintViolations {

    private ConstraintViolations() {}

    public static Optional<String> constraintName(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return Optional.of(violation.getConstraintName().toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }
}
