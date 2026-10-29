package atf.shop.tests.support.order;

import atf.reporting.api.ReportModelAdapters;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.ui.UiExtentReportSession;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.locators.reporting.ShopOrderReportLocators;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.Page;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Consumer-specific projection of an observed order checkpoint; canonical test reports remain owned by ShopComponentTest. */
public final class ShopOrderReportProbe {
    private static final ShopOrderReportLocators LOCATORS = new ShopOrderReportLocators();
    private ShopOrderReportProbe() { }

    public static void verify(Page report, Page application, ShopPurchaseCheckpoint checkpoint,
                              PlaywrightActionSequence actions, boolean fallback, String label) throws Exception {
        var path = ShopConfigurationLoader.load().uiReportDirectory().resolve("order-probes")
                .resolve("order-" + label + "-" + (fallback ? "fallback-" : "extent-") + UUID.randomUUID() + ".html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(path) : new ExtentReportSession(path)) {
            var scenario = UiExtentReportSession.adaptScenario(session.startScenario("ShopOrderEvidence", label,
                            List.of("shop-order-probe")),
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT), label);
            var phase = scenario.startPhase("TEST BODY");
            phase.step(checkpoint.keyword(), checkpoint.title(), application.url(),
                    Base64.getEncoder().encodeToString(application.screenshot()), checkpoint.assertions(),
                    ReportModelAdapters.uiActions(actions.snapshot()), checkpoint.apiValidation(),
                    ReportScreenshotPolicy.CapturePoint.FAILURE, AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS); scenario.complete(AtfReportStatus.PASS);
        }
        assertFalse(Files.readString(path).contains("raw-order-secret"), "Captured response secrets stay redacted");
        report.navigate(path.toUri().toString());
        LOCATORS.tab(report, fallback, "Tests").click();
        if (!fallback) LOCATORS.expandAll(report).click();
        for (var summary : LOCATORS.summaries(report)) {
            if (summary.innerText().trim().startsWith("Assertions")) summary.press("Enter");
        }
        var response = LOCATORS.responseComparison(report);
        assertEquals(1, response.count(), "One visible canonical comparison, excluding collapsed diagnostic copies");
        assertTrue(response.isVisible(), "Current order comparison must remain inspectable");
        String actual = LOCATORS.cells(response).nth(2).innerText();
        assertTrue(actual.length() <= 240);
        assertEquals(fallback ? AtfReportStatus.FAIL.displayLabel() : AtfReportStatus.FAIL.marker(),
                LOCATORS.cells(response).last().innerText(), "The renderer's canonical result must remain FAIL");
        if (checkpoint.apiEvidence().isEmpty()) {
            assertEquals("missing", actual);
            assertEquals(0, LOCATORS.responseDetails(report).count());
        } else {
            int status = checkpoint.apiEvidence().iterator().next().response().status();
            assertTrue(actual.contains(status < 0 ? "Unavailable" : "500"));
            var details = LOCATORS.responseDetails(report);
            assertEquals(1, details.count());
            details.press("Enter");
            if (status == 500) assertTrue(LOCATORS.parent(details).innerText().contains("current order failed"));
            details.press("Space");
        }
        LOCATORS.tab(report, fallback, "Fails").click();
        var backlink = LOCATORS.failureLink(report, fallback);
        assertTrue(backlink.getAttribute("href").startsWith("#test-id="));
        backlink.click();
        assertTrue(report.url().contains("test-id="), "Backlink targets the owning scenario");
        if (!fallback) LOCATORS.expandAll(report).click();
        assertTrue(response.isVisible(), "Failure backlink returns to the observed order checks");
        LOCATORS.tab(report, fallback, "Overview").click();
        assertEquals("1", LOCATORS.failedMetric(report, fallback).innerText(), "Caller PASS cannot override the order FAIL");
    }
}
