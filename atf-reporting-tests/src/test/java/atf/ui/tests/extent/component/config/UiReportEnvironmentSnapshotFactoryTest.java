package atf.ui.tests.extent.component.config;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.config.environment.UiReportEnvironmentSnapshotFactory;
import atf.ui.config.UiBrowserConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import static atf.reporting.configuration.ReportEnvironmentField.API_EVIDENCE;
import static atf.reporting.configuration.ReportEnvironmentField.REPORT_ARTIFACT;
import static atf.reporting.configuration.ReportEnvironmentField.SUITE;
import static atf.reporting.configuration.ReportEnvironmentField.VIEWPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class UiReportEnvironmentSnapshotFactoryTest {
    @Test
    void publishesResolvedViewportAndCssScaleInsteadOfDefaults() {
        var browser = new UiBrowserConfiguration(URI.create("https://example.test"), "chromium", true,
                Duration.ofSeconds(12), Duration.ofSeconds(24), 1280, 720, 70, true);
        var resolved = new UiConfiguration(browser, Path.of("target"), "audit_");

        var snapshot = UiReportEnvironmentSnapshotFactory.audit(resolved,
                "Override audit", "embedded pointer fixture");

        assertEquals("1280x720 @ 70%", snapshot.values().get(VIEWPORT));
        assertEquals("1280x720 @ 70%", snapshot.systemDisplayValues().get(VIEWPORT.label()));
    }

    @Test
    void buildsAllowlistedMetadataWithoutRawConfigurationKeys() {
        var snapshot = UiReportEnvironmentSnapshotFactory.audit(
                UiConfiguration.defaults(Path.of("target")),
                "Extent UI audit", "artifact inspection");

        assertTrue(snapshot.values().containsKey(SUITE));
        assertTrue(snapshot.values().containsKey(API_EVIDENCE));
        assertTrue(snapshot.values().containsKey(REPORT_ARTIFACT));
        assertFalse(snapshot.displayValues().containsKey("ui.retry.maxAttempts"));
        assertFalse(snapshot.displayValues().containsKey("raw.yaml.key"));
    }
}
