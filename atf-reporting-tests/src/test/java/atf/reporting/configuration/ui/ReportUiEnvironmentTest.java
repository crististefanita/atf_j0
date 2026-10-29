package atf.reporting.configuration.ui;

import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@AtfUnitTest
class ReportUiEnvironmentTest {
    @Test void commonUiProjectionUsesResolvedPoliciesAndTheShopVocabulary() {
        var profile = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW).actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY).build();
        var input = new ReportUiEnvironment("QA", URI.create("https://user:private-password@fixture.test:8443/hidden?q=private#secret"),
                new ReportUiEnvironment.Browser("chromium", true, 1440, 1000, 70, Duration.ofSeconds(20), Duration.ofSeconds(45)),
                "UI evidence", "disabled", profile, "Controlled fixture");
        var values = input.describe().build().values();
        assertEquals(13, values.size());
        assertEquals("Playwright browser", values.get(ReportEnvironmentField.CLIENT));
        assertEquals("chromium · headless · viewport 1440 × 1000 · zoom 70%", values.get(ReportEnvironmentField.BROWSER));
        assertEquals("Final screenshot only", values.get(ReportEnvironmentField.SCREENSHOT_POLICY));
        assertEquals("One consolidated flow", values.get(ReportEnvironmentField.UI_EVIDENCE_LAYOUT));
        assertEquals("summary-only", values.get(ReportEnvironmentField.UI_ACTION_DETAILS));
        assertEquals("20s", values.get(ReportEnvironmentField.UI_TIMEOUT));
        assertEquals("https://fixture.test:8443", values.get(ReportEnvironmentField.UI_HOST));
        assertFalse(values.toString().contains("private"));
        var independent = input.describe().put(ReportEnvironmentField.ENVIRONMENT, "OTHER").build();
        assertNotEquals(independent.values().get(ReportEnvironmentField.ENVIRONMENT), input.describe().build().values().get(ReportEnvironmentField.ENVIRONMENT));
    }

    @Test void originsAndDurationsRemainOperationalWithoutInventedHostsOrSeconds() {
        assertEquals("configured", ReportUiEnvironment.origin(URI.create("about:blank")));
        assertEquals("http://[::1]:9000", ReportUiEnvironment.origin(URI.create("http://[::1]:9000/private")));
        assertEquals("250ms", ReportUiEnvironment.duration(Duration.ofMillis(250)));
        assertEquals("2s", ReportUiEnvironment.duration(Duration.ofSeconds(2)));
    }
}
