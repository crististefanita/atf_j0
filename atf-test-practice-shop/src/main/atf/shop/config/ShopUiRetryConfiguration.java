package atf.shop.config;

import atf.ui.config.UiRetryOperation;
import atf.ui.config.UiRetryPolicy;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consumer-owned, typed retry configuration for Practice Shop browser checks.
 *
 * <p>There is intentionally no redundant {@code enabled} flag.  A policy is
 * effective only when it has more than one attempt and at least one eligible
 * read-only operation; this keeps the YAML fail-closed without creating a
 * boolean that can contradict the actual policy.</p>
 */
public record ShopUiRetryConfiguration(
        int maxAttempts,
        Duration backoff,
        Set<UiRetryOperation> eligibleOperations,
        String reason) {

    public ShopUiRetryConfiguration {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("practiceShop.retry.maxAttempts must be positive");
        }
        backoff = Objects.requireNonNull(backoff, "backoff");
        if (backoff.isNegative()) {
            throw new IllegalArgumentException("practiceShop.retry.backoff must not be negative");
        }
        eligibleOperations = eligibleOperations == null ? Set.of() : Set.copyOf(eligibleOperations);
        reason = reason == null || reason.isBlank()
                ? UiRetryPolicy.DEFAULT_REASON : reason.trim();
    }

    public static ShopUiRetryConfiguration none() {
        return new ShopUiRetryConfiguration(
                UiRetryPolicy.DEFAULT_MAX_ATTEMPTS,
                UiRetryPolicy.DEFAULT_BACKOFF,
                Set.of(),
                "disabled");
    }

    public UiRetryPolicy policy() {
        if (!enabled()) return UiRetryPolicy.none();
        return new UiRetryPolicy(maxAttempts, backoff, eligibleOperations, reason);
    }

    public boolean enabled() {
        return maxAttempts > UiRetryPolicy.DEFAULT_MAX_ATTEMPTS
                && !eligibleOperations.isEmpty();
    }

    public String summary() {
        if (!enabled()) return "disabled; attempts=1; eligible=none";
        String operations = eligibleOperations.stream()
                .map(UiRetryOperation::configName)
                .sorted()
                .collect(Collectors.joining(","));
        return "enabled; attempts=" + maxAttempts
                + "; backoff=" + backoff
                + "; eligible=" + operations
                + "; reason=" + reason;
    }
}
