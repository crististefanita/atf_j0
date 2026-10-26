package atf.core.config;

import java.util.Objects;

    /** Minimal configuration boundary for an ATF module. */
public record AtfConfiguration(ApiSettings api, ReportingSettings reporting) {
    public AtfConfiguration {
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(reporting, "reporting");
    }

    public static AtfConfiguration defaults(java.net.URI baseUri) {
        return new AtfConfiguration(new ApiSettings(baseUri, "none"), ReportingSettings.defaults());
    }

    public void validate() {
        snapshot();
    }

    /** Captures all runtime policies once at the composition boundary. */
    public AtfRuntimeSnapshot snapshot() {
        return new AtfRuntimeSnapshot(api.snapshot(), reporting);
    }
}
