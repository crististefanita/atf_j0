package atf.ui.tests.extent.component.config;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.config.environment.UiReportEnvironmentSnapshotFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static atf.reporting.configuration.ReportEnvironmentField.API_EVIDENCE;
import static atf.reporting.configuration.ReportEnvironmentField.REPORT_ARTIFACT;
import static atf.reporting.configuration.ReportEnvironmentField.SUITE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class UiReportEnvironmentSnapshotFactoryTest {
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
