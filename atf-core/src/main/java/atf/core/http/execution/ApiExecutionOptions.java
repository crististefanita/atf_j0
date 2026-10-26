package atf.core.http.execution;

import java.time.Duration;

    /** Per-execution budget; it is immutable and does not change global configuration. */
public record ApiExecutionOptions(Duration timeout) {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public ApiExecutionOptions {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    public static ApiExecutionOptions defaults() {
        return new ApiExecutionOptions(DEFAULT_TIMEOUT);
    }
}
