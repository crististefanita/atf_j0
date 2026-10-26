package atf.core;
import atf.testkit.AtfUnitTest;


import atf.core.config.ApiSettings;
import atf.core.config.AtfConfiguration;
import atf.core.config.AtfRuntimeSnapshot;
import atf.core.config.ReportingSettings;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class ConfigurationContractTest {
    @Test
    void settingsSnapshotIsImmutableAndCopiesRetryStatuses() {
        var statuses = new java.util.HashSet<>(Set.of(429));
        var settings = new ApiSettings(URI.create("https://example.test"), "default",
                Duration.ofSeconds(3), statuses, 3);
        var snapshot = settings.snapshot();
        statuses.add(503);

        assertEquals(Set.of(429), snapshot.retryStatuses());
        assertEquals(Duration.ofSeconds(3), snapshot.timeout());
        assertEquals(3, snapshot.maxAttempts());
    }

    @Test
    void configurationRejectsNullBoundariesAndValidatesDisplayLimit() {
        var configuration = new AtfConfiguration(
                new ApiSettings(URI.create("https://example.test"), "none"),
                ReportingSettings.defaults());
        assertDoesNotThrow(configuration::validate);
        assertThrows(NullPointerException.class, () -> new AtfConfiguration(null, ReportingSettings.defaults()));
    }

    @Test
    void runtimeSnapshotOwnsIndependentApiAndReportingPolicies() {
        var configuration = new AtfConfiguration(
                new ApiSettings(URI.create("https://snapshot.example"), "default",
                        Duration.ofSeconds(7), Set.of(429, 503), 3),
                new ReportingSettings(128));

        AtfRuntimeSnapshot snapshot = configuration.snapshot();

        assertEquals(URI.create("https://snapshot.example"), snapshot.api().baseUri());
        assertEquals(Duration.ofSeconds(7), snapshot.api().timeout());
        assertEquals(Set.of(429, 503), snapshot.api().retryStatuses());
        assertEquals(3, snapshot.api().maxAttempts());
        assertEquals(128, snapshot.reporting().assertionMaxDisplayCharacters());
        assertEquals(12_000, snapshot.reporting().bodyMaxDisplayCharacters());
    }

    @Test
    void reportingSettingsRejectInvalidBodyDisplayLimits() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ReportingSettings(240, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ReportingSettings(240, -1)),
                () -> assertEquals(12_000, new ReportingSettings(240).bodyMaxDisplayCharacters()));
    }
}
