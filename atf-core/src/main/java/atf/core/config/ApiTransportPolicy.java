package atf.core.config;

import java.util.Objects;

/**
 * Immutable transport safety policy resolved once at the composition root.
 * Live transport requires an explicit opt-in; acceptance never does.
 */
public record ApiTransportPolicy(ApiTransportMode mode, boolean liveOptIn) {
    public ApiTransportPolicy {
        mode = Objects.requireNonNull(mode, "mode");
        if (mode == ApiTransportMode.LIVE && !liveOptIn) {
            throw new IllegalArgumentException(
                    "Live API transport requires explicit opt-in; acceptance remains the default");
        }
        if (mode == ApiTransportMode.ACCEPTANCE && liveOptIn) {
            throw new IllegalArgumentException("Live opt-in cannot be set for acceptance transport");
        }
    }

    public static ApiTransportPolicy acceptance() {
        return new ApiTransportPolicy(ApiTransportMode.ACCEPTANCE, false);
    }

    public static ApiTransportPolicy explicitLive() {
        return new ApiTransportPolicy(ApiTransportMode.LIVE, true);
    }
}
