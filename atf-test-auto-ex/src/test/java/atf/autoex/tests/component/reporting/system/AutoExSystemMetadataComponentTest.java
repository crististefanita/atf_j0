package atf.autoex.tests.component.reporting.system;

import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.reporting.AutoExReportSystemMetadata;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;

@AutoExComponentTest
class AutoExSystemMetadataComponentTest {
    @Test
    void autoExOverviewIncludesEffectiveUiPolicyInsteadOfOnlyFourBrowserFields() throws Exception {
        var configuration = AutoExConfigurationLoader.load();
        var profile = ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY);
        Path output = Files.createDirectories(Path.of("target", "system-metadata-proof", UUID.randomUUID().toString()));
        Path file;
        try (var owner = AutoExReportSession.open(output, "auto-system.html", AtfConsoleSink.noOp(), profile, configuration.browser())) {
            file = owner.report().reportPath();
            var scenario = owner.report().startScenario("AutoExSystemFixture", getClass().getPackageName(),
                    "effective UI policy", List.of("component"), profile);
            scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(file);
        assertTrue(html.contains(ReportEnvironmentField.UI_TIMEOUT.label()), "System must publish the configured UI timeout");
        assertTrue(html.contains(ReportEnvironmentField.SCREENSHOT_POLICY.label()), "System must publish the effective screenshot policy");
    }

    @Test void resolvedScenarioAxesAndApiCaptureSettingsArePublishedTruthfully() {
        var configuration = AutoExE2eRun.withNetworkInspection(AutoExConfigurationLoader.load());
        var effective = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW).actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY).build();
        var values = AutoExReportSystemMetadata.snapshot(configuration, effective, true, "QA").values();
        assertEquals(20, values.size());
        assertEquals("QA", values.get(ReportEnvironmentField.ENVIRONMENT));
        assertEquals("UI + API evidence", values.get(ReportEnvironmentField.PROFILE));
        assertEquals("Final screenshot only", values.get(ReportEnvironmentField.SCREENSHOT_POLICY));
        assertEquals("One consolidated flow", values.get(ReportEnvironmentField.UI_EVIDENCE_LAYOUT));
        assertEquals("summary-only", values.get(ReportEnvironmentField.UI_ACTION_DETAILS));
        assertTrue(values.get(ReportEnvironmentField.BROWSER).contains("zoom 70%"));
        assertTrue(values.get(ReportEnvironmentField.API_TIMEOUT).contains("Browser-managed; observation wait 5s"));
        assertTrue(values.get(ReportEnvironmentField.NETWORK_INSPECTOR).startsWith("Enabled"));
        assertFalse(values.toString().contains(configuration.payment().cardNumber()));
    }

    @Test void uiOnlyHasTheCommonUiFieldsWithoutClaimingApiEvidence() {
        var configuration = AutoExConfigurationLoader.load();
        var values = AutoExReportSystemMetadata.snapshot(configuration, ReportEvidenceProfile.defaults(), false, "LOCAL").values();
        assertEquals(13, values.size());
        assertEquals("UI evidence", values.get(ReportEnvironmentField.PROFILE));
        assertFalse(values.containsKey(ReportEnvironmentField.API_ENDPOINT));
        assertFalse(values.containsKey(ReportEnvironmentField.TRANSPORT_MODE));
        assertFalse(values.containsKey(ReportEnvironmentField.API_TIMEOUT));
        assertFalse(values.containsKey(ReportEnvironmentField.BODY_PREVIEW));
    }
}
