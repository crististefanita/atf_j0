package atf.ui.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Bounded wait policy for a server mutation observed from a browser action.
 *
 * <p>This policy does not replay an action.  It only defines how long a
 * consumer may wait for the response/read-back boundary before continuing
 * the UI journey.  Action replay belongs to the consumer because only the
 * consumer knows whether the mutation is idempotent.</p>
 */
public record UiNetworkSettlementPolicy(
        boolean enabled,
        Duration timeout,
        Duration pollInterval) {

    public UiNetworkSettlementPolicy {
        timeout = positive(timeout, "ui.network.settlement.timeout");
        pollInterval = positive(pollInterval, "ui.network.settlement.pollInterval");
        if (pollInterval.compareTo(timeout) > 0) {
            throw new IllegalArgumentException(
                    "ui.network.settlement.pollInterval must not exceed timeout");
        }
    }

    public static UiNetworkSettlementPolicy disabled() {
        return new UiNetworkSettlementPolicy(false, Duration.ofSeconds(10), Duration.ofMillis(100));
    }

    public static UiNetworkSettlementPolicy bounded(Duration timeout, Duration pollInterval) {
        return new UiNetworkSettlementPolicy(true, timeout, pollInterval);
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
