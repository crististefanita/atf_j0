package atf.core.config;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

    /** Immutable copy used by an executor; the host cannot modify it. */
public record ApiSettingsSnapshot(URI baseUri, String defaultAuthProfile, Duration timeout,
                                  Set<Integer> retryStatuses, int maxAttempts) {
    public ApiSettingsSnapshot {
        retryStatuses = retryStatuses == null ? Set.of() : Set.copyOf(retryStatuses);
    }
}
