package atf.core.config;

import java.util.Objects;

/**
 * Immutable runtime boundary produced once by the composition root.
 *
 * <p>Execution and reporting receive projections of this snapshot instead of
 * re-reading mutable configuration. Keeping the API and reporting policies in
 * one value makes parallel runs explicit without coupling core to YAML or
 * Extent.</p>
 */
public record AtfRuntimeSnapshot(ApiSettingsSnapshot api, ReportingSettings reporting) {
    public AtfRuntimeSnapshot {
        api = Objects.requireNonNull(api, "api");
        reporting = Objects.requireNonNull(reporting, "reporting");
    }
}
