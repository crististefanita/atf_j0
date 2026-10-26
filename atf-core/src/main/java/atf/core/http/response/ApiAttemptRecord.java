package atf.core.http.response;

import java.time.Duration;
import java.util.Objects;

/** Immutable, renderer-neutral evidence for one completed API attempt. */
public record ApiAttemptRecord(
        int number,
        int statusCode,
        Duration duration,
        ApiAttemptOutcome outcome,
        String reason) {

    public ApiAttemptRecord {
        if (number < 1) throw new IllegalArgumentException("number must be positive");
        if (statusCode < 0) throw new IllegalArgumentException("statusCode must not be negative");
        duration = Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("duration must not be negative");
        outcome = Objects.requireNonNull(outcome, "outcome");
        reason = reason == null ? "" : reason;
    }
}
