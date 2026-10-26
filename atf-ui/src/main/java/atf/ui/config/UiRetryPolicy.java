package atf.ui.config;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable action-level retry policy for Playwright UI checks.
 *
 * <p>The policy is deliberately fail-closed: the default has one attempt and
 * no eligible operation. It can only retry the explicitly listed, read-only
 * operations; it never replays a whole test or mutating browser action.</p>
 */
public record UiRetryPolicy(int maxAttempts, Duration backoff,
                            Set<UiRetryOperation> eligibleOperations, String reason) {
    public static final int DEFAULT_MAX_ATTEMPTS = 1;
    public static final Duration DEFAULT_BACKOFF = Duration.ZERO;
    public static final String DEFAULT_REASON = "bounded read-only UI retry";

    public UiRetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("ui.retry.maxAttempts must be positive");
        }
        backoff = Objects.requireNonNull(backoff, "backoff");
        if (backoff.isNegative()) {
            throw new IllegalArgumentException("ui.retry.backoff must not be negative");
        }
        eligibleOperations = eligibleOperations == null || eligibleOperations.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(eligibleOperations));
        reason = reason == null || reason.isBlank() ? DEFAULT_REASON : reason.trim();
    }

    public static UiRetryPolicy none() {
        return new UiRetryPolicy(DEFAULT_MAX_ATTEMPTS, DEFAULT_BACKOFF, Set.of(),
                "disabled");
    }

    public static UiRetryPolicy readOnly(int maxAttempts, Duration backoff) {
        return new UiRetryPolicy(maxAttempts, backoff,
                EnumSet.allOf(UiRetryOperation.class), DEFAULT_REASON);
    }

    public boolean enabled() {
        return maxAttempts > DEFAULT_MAX_ATTEMPTS && !eligibleOperations.isEmpty();
    }

    public boolean allows(String operation, int attempt) {
        if (!enabled() || attempt < 1 || attempt >= maxAttempts) return false;
        return eligibleOperations.stream().anyMatch(candidate -> candidate.label().equals(operation));
    }

    public void pause() {
        if (backoff.isZero()) return;
        try {
            Thread.sleep(backoff.toMillis(), backoff.getNano() % 1_000_000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("UI retry backoff was interrupted", interrupted);
        }
    }
}
