package atf.reporting.ui;
import atf.testkit.AtfUnitTest;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards the lifecycle composition boundary without changing the public facade. */
@AtfUnitTest
class UiExtentReportSessionCompositionTest {

    @Test
    void typedOptionsKeepCompositionValuesTogether() {
        var profile = ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY);
        var options = UiExtentReportOptions.builder(Path.of("target/reports"))
                .fileName("ui.html")
                .console(AtfConsoleSink.noOp())
                .evidenceProfile(profile)
                .timestampEnabled(false)
                .build();

        assertAll(
                () -> assertEquals(Path.of("target/reports"), options.outputDirectory()),
                () -> assertEquals("ui.html", options.fileName()),
                () -> assertEquals(profile, options.evidenceProfile()),
                () -> assertFalse(options.timestampEnabled()));
    }

    @Test
    void typedEnvironmentKeepsOverviewConfigurationOnAnAllowlist() {
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "LOCAL")
                .put(ReportEnvironmentField.UI_TIMEOUT, "20 s")
                .put(ReportEnvironmentField.TEST_PACKAGE, "atf.ui.tests.extent")
                .put(ReportEnvironmentField.SUITE, "Extent UI audit")
                .put(ReportEnvironmentField.FLOW, "report inspection")
                .put(ReportEnvironmentField.FIXTURE, "ui-audit-fixture")
                .put(ReportEnvironmentField.RETRY_POLICY, "read-only; max=2")
                .build();
        var options = UiExtentReportOptions.builder(Path.of("target/reports"))
                .environment(environment)
                .build();

        assertAll(
                () -> assertEquals("LOCAL",
                        options.environment().displayValues().get("Environment")),
                () -> assertEquals("20 s",
                         options.environment().displayValues().get("UI action timeout")),
                () -> assertFalse(options.environment().systemDisplayValues().containsKey("Test package")),
                () -> assertFalse(options.environment().systemDisplayValues().containsKey("Suite")),
                () -> assertFalse(options.environment().systemDisplayValues().containsKey("Flow")),
                () -> assertFalse(options.environment().systemDisplayValues().containsKey("Fixture")),
                () -> assertFalse(options.environment().displayValues().containsKey("raw.yaml.key")));
    }

}
