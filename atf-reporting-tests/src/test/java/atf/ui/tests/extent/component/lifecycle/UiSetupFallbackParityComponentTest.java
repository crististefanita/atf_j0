package atf.ui.tests.extent.component.lifecycle;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import atf.ui.tests.extent.support.locators.failurecontext.ReportFailureContextLocators;
import atf.ui.tests.extent.support.locators.preview.ReportPreviewLocators;
import atf.ui.tests.extent.support.locators.verdict.VerdictReportLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
@ReportBrowserTest
class UiSetupFallbackParityComponentTest {
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
    private static final ReportUiLocators COMMON = new ReportUiLocators();
    private static final ReportDisclosureLocators DISCLOSURES = new ReportDisclosureLocators();
    private static final ReportFailureContextLocators CONTEXT = new ReportFailureContextLocators();
    private static final ReportPreviewLocators PREVIEW = new ReportPreviewLocators();
    private static final VerdictReportLocators VIEWS = new VerdictReportLocators();

    @ParameterizedTest @CsvSource({"false,240", "true,240", "false,18", "true,18"})
    void setupComparisonsActionsAndCaptureRemainInspectableAndBounded(boolean fallback, int limit) throws Exception {
        var file = TestReportPaths.report("setup-ui-parity-" + fallback + "-" + limit + "-" + System.nanoTime() + ".html");
        var settings = new ReportingSettings(limit, 18);
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, settings) : new ExtentReportSession(file, settings)) {
            var scenario = UiExtentReportSession.adaptScenario(session.startScenario("UiSetup", "auditable setup", List.of("component")),
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT), "auditable setup");
            var setup = scenario.startPhase("SETUP");
            String value = "password=raw-setup-password; " + "long setup value ".repeat(35);
            setup.setup(AtfGherkinKeyword.GIVEN, "configured inputs are checked", "#setup",
                    List.of(new AtfAssertion("configured input", value, value, true),
                            new AtfAssertion("difference beyond preview", "same prefix ".repeat(50) + "expected", "same prefix ".repeat(50) + "actual", false)),
                    List.of(new AtfUiActionRecord(1, "Fill", "#setup", "input is ready", value, 7, AtfReportStatus.PASS)),
                    UiApiValidationEvidence.empty(), new ReportScreenshot("setup capture", PNG), AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS); scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(file);
        assertFalse(html.contains("raw-setup-password"));
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                page.navigate(file.toUri().toString()); VIEWS.tests(page, fallback).click();
                if (!fallback) { DISCLOSURES.firstOwner(page).click(); VIEWS.expandAll(page).click(); }
                var scenario = VIEWS.scenario(page, fallback, "auditable setup");
                var checks = COMMON.setupEvidence(scenario);
                assertEquals(1, checks.count(), "setup comparisons must be rendered by both hosts");
                assertFalse(Boolean.TRUE.equals(checks.evaluate("d=>d.open")));
                DISCLOSURES.summary(checks).press("Enter");
                var rows = COMMON.setupEvidenceRows(checks); assertEquals(2, rows.count());
                for (var row : rows.all()) for (int column : List.of(1, 2)) {
                    String value = CONTEXT.cells(row).nth(column).innerText();
                    assertTrue(value.length() <= limit, "setup comparison exceeds configured preview budget");
                    assertTrue(value.endsWith("…"));
                }
                assertTrue(CONTEXT.cells(rows.nth(1)).last().innerText().contains("❌"), "preview equality cannot replace an evaluated FAIL");
                var actions = CONTEXT.actionDetails(scenario); assertEquals(1, actions.count());
                DISCLOSURES.summary(actions).press("Enter");
                assertEquals(1, CONTEXT.actionRows(page).count());
                assertTrue(CONTEXT.cells(CONTEXT.actionRows(page).first()).nth(3).innerText().length() <= limit);
                assertTrue(DISCLOSURES.screenshots(scenario).first().isVisible(), "the real capture remains accessible");
                assertTrue(html.contains(PNG), "the artifact contains the supplied capture payload");
                DISCLOSURES.summary(actions).press("Enter"); DISCLOSURES.summary(actions).press("Space");
                assertTrue(Boolean.TRUE.equals(actions.evaluate("d=>d.open")));
                assertEquals(0, CONTEXT.httpDetails(scenario).count(), "setup does not fabricate HTTP evidence");
                VIEWS.overview(page, fallback).click();
                assertEquals("1", VIEWS.metric(page, fallback, fallback ? "Failed" : "Tests Failed").innerText(), "setup FAIL survives caller PASS");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}
