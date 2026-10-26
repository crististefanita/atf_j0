package atf.reporting.standalone;

import atf.reporting.configuration.ReportEnvironment;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.security.SensitiveDataRedactor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Standalone proof for typed operational metadata used by Overview/System. */
class ReportingStandaloneEnvironmentSnapshotTest {
    @Test
    void publishesClientAsBrowserMetadataWithoutPublishingTestIdentity() {
        var snapshot = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.CLIENT, "Playwright browser")
                .put(ReportEnvironmentField.SUITE, "internal suite")
                .build();

        var systemInfo = ReportEnvironment.with(snapshot, new SensitiveDataRedactor());

        assertAll(
                () -> assertEquals("Client", ReportEnvironmentField.CLIENT.label()),
                () -> assertEquals("Browser", ReportEnvironmentField.CLIENT.group()),
                () -> assertTrue(ReportEnvironmentField.CLIENT.isSystemVisible()),
                () -> assertEquals("Playwright browser",
                        snapshot.displayValues().get(ReportEnvironmentField.CLIENT.label())),
                () -> assertEquals("Playwright browser",
                        systemInfo.get(ReportEnvironmentField.CLIENT.label())),
                () -> assertFalse(systemInfo.containsKey(ReportEnvironmentField.SUITE.label())));
    }
}
