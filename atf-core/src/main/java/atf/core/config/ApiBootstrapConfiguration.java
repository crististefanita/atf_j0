package atf.core.config;

import java.util.Objects;

/** Complete typed API composition input captured before a run starts. */
public record ApiBootstrapConfiguration(ApiSettings settings,
                                        ApiTransportPolicy transportPolicy) {
    public ApiBootstrapConfiguration {
        settings = Objects.requireNonNull(settings, "settings");
        transportPolicy = Objects.requireNonNull(transportPolicy, "transportPolicy");
    }

    public static ApiBootstrapConfiguration acceptance(ApiSettings settings) {
        return new ApiBootstrapConfiguration(settings, ApiTransportPolicy.acceptance());
    }

    public static ApiBootstrapConfiguration live(ApiSettings settings) {
        return new ApiBootstrapConfiguration(settings, ApiTransportPolicy.explicitLive());
    }

    public ApiSettingsSnapshot snapshot() {
        return settings.snapshot();
    }
}
