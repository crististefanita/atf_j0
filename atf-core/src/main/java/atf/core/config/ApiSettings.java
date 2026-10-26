package atf.core.config;

import atf.core.http.auth.AuthProfiles;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.Objects;

public record ApiSettings(URI baseUri, String defaultAuthProfile, Duration timeout,
                          Set<Integer> retryStatuses, int maxAttempts) {
    public ApiSettings(URI baseUri, String defaultAuthProfile) {
        this(baseUri, defaultAuthProfile, Duration.ofSeconds(30), Set.of(429), 1);
    }

    public ApiSettings {
        Objects.requireNonNull(baseUri, "baseUri");
        if (defaultAuthProfile == null || defaultAuthProfile.isBlank()) {
            defaultAuthProfile = AuthProfiles.NONE;
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        retryStatuses = retryStatuses == null ? Set.of() : Set.copyOf(retryStatuses);
        if (maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts must be positive");
    }

    public ApiSettingsSnapshot snapshot() {
        return new ApiSettingsSnapshot(baseUri, defaultAuthProfile, timeout, retryStatuses, maxAttempts);
    }
}
