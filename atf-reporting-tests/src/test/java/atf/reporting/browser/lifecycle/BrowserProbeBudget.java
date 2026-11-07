package atf.reporting.browser.lifecycle;

import java.time.Duration;
import java.util.Objects;

/** Setup and cleanup do not borrow the browser's action/navigation timeouts. */
public record BrowserProbeBudget(Duration setup, Duration verification, Duration cleanup, Duration lifetime) {
    public static final BrowserProbeBudget DEFAULT = new BrowserProbeBudget(
            Duration.ofSeconds(30), Duration.ofSeconds(60), Duration.ofSeconds(5));

    public BrowserProbeBudget(Duration setup, Duration verification, Duration cleanup) {
        this(setup, verification, cleanup, setup.plus(verification).plus(cleanup));
    }

    public BrowserProbeBudget {
        for (Duration value : new Duration[] {setup, verification, cleanup, lifetime}) {
            Objects.requireNonNull(value, "phase budget");
            if (value.isZero() || value.isNegative()) throw new IllegalArgumentException("phase budget must be positive");
            value.toNanos();
        }
    }

    public Duration forPhase(String phase) {
        return switch (phase) {
            case "DISCOVERY", "VERIFY" -> verification;
            case "CLEANUP", "DONE", "FAILED" -> cleanup;
            default -> setup;
        };
    }
}
