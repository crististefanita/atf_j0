package atf.reporting.audit;

import java.util.List;
import java.util.Objects;

/** Renderer-neutral retry facts extracted from one API request node. */
public record RetryEvidenceSnapshot(int attempts, int retryEvents, int finalStatus,
                                    List<Attempt> history) {
    public RetryEvidenceSnapshot {
        if (attempts < 0 || retryEvents < 0) {
            throw new IllegalArgumentException("retry counts must be non-negative");
        }
        history = List.copyOf(Objects.requireNonNull(history, "history"));
    }

    public boolean hasRetry() { return retryEvents > 0 || attempts > 1; }

    public record Attempt(int number, int status, String outcome) {
        public Attempt {
            if (number < 1 || status < 0) {
                throw new IllegalArgumentException("attempt number/status must be positive");
            }
            outcome = Objects.requireNonNull(outcome, "outcome");
        }
    }
}
