package atf.ui.config.recovery;

import java.time.Duration;
import java.util.*;

/** Immutable ceilings, independent of UI/UI_API presentation and ordinary locator retries. */
public record UiRecoveryConfiguration(boolean enabled, String profile, Set<String> operations, int maxAttempts,
                                       Duration totalTimeout, Duration backoff, boolean captureBackendSignals, String source) {
    public static final String READ_ONLY_PROFILE = "read-only";
    public UiRecoveryConfiguration {
        if (!READ_ONLY_PROFILE.equals(profile)) throw new IllegalArgumentException("Unknown recovery profile: " + profile);
        operations = Set.copyOf(Objects.requireNonNull(operations, "operations"));
        if (maxAttempts < 1) throw new IllegalArgumentException("recovery.maxAttempts must include the first positive attempt");
        Objects.requireNonNull(totalTimeout, "totalTimeout"); Objects.requireNonNull(backoff, "backoff");
        if (totalTimeout.isNegative() || totalTimeout.isZero() || backoff.isNegative()) throw new IllegalArgumentException("Invalid recovery time budget");
        totalTimeout.toNanos(); backoff.toNanos();
        if (backoff.compareTo(totalTimeout) >= 0) throw new IllegalArgumentException("recovery.backoff must be below totalTimeout");
        if (enabled && operations.isEmpty()) throw new IllegalArgumentException("Enabled recovery requires an explicit operation allowlist");
        source = Objects.requireNonNull(source, "source");
    }
    public static UiRecoveryConfiguration disabled() {
        return new UiRecoveryConfiguration(false, READ_ONLY_PROFILE, Set.of(), 1, Duration.ofSeconds(5), Duration.ofMillis(100), false, "YAML");
    }
    public UiRecoveryConfiguration restrict(boolean declarationAllows, String origin) {
        String resolvedSource = "YAML".equals(Objects.requireNonNull(origin)) ? "YAML" : "YAML; " + origin;
        return new UiRecoveryConfiguration(enabled && declarationAllows, profile, operations, maxAttempts,
                totalTimeout, backoff, captureBackendSignals, resolvedSource);
    }
    public void requireKnownReadOnlyOperations(Set<String> readOnlyOperations) {
        var unknown = new TreeSet<>(operations); unknown.removeAll(readOnlyOperations);
        if (!unknown.isEmpty()) throw new IllegalArgumentException("Recovery contains unknown or mutating operations: " + unknown);
    }
}
