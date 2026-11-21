package atf.ui.tests.extent.support.inspection.assertions;

import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.security.SensitiveDataRedactor;
import atf.ui.tests.extent.support.locators.disclosure.ReportBulkDisclosureLocators;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Browser proof for phase observations alongside the real component assertion step. */
public final class AssertionAuditObservationDisclosureProbe {
    private static final ReportBulkDisclosureLocators TREE = new ReportBulkDisclosureLocators();

    private AssertionAuditObservationDisclosureProbe() { }

    public static void verify() throws Exception {
        verify(publishFixture(), true);
    }

    /** Opens a report published by a consumer's real JUnit extension. */
    public static void verify(Path report) throws Exception {
        if (!Files.isRegularFile(report)) throw new IllegalArgumentException("Report is not a regular file: " + report);
        verify(report, false);
    }

    private static void verify(Path report, boolean fixture) throws Exception {
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(),
                     new BrowserType.LaunchOptions().setHeadless(true)))) {
            try {
                Browser browser = browserLease.value();
                Page page = BrowserResources.newPage(browser);
                page.setDefaultTimeout(5_000);
                var errors = new ArrayList<String>();
                page.onPageError(errors::add);
                page.navigate(report.toUri() + "#test-view");
                TREE.owners(page).first().click();
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                TREE.expand(page).click();

                var activeDetail = TREE.detail(page);
                var evidence = activeDetail.locator("details.atf-observation-evidence");
                assertTrue(evidence.count() > 0, "the report contains phase-scoped observation disclosures");
                if (fixture) assertEquals(1, evidence.count());
                assertFalse((Boolean) evidence.evaluateAll("nodes => nodes.some(node => node.open)"),
                        "bulk tree expansion leaves nested observation evidence collapsed");
                var exactText = new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true);
                assertTrue(activeDetail.getByText("TEST BODY", exactText).first().isVisible());
                assertTrue(activeDetail.getByText("Component assertions", exactText).first().isVisible());
                var assertions = activeDetail.locator("div.atf-ui-assertions table.helm-validation-table");
                assertTrue(assertions.count() > 0, "expanded assertion steps expose their validation tables");
                assertTrue(assertions.first().isVisible());
                assertTrue(assertions.first().locator("tbody tr").count() > 0);

                for (int index = 0; index < evidence.count(); index++) {
                    var disclosure = evidence.nth(index);
                    var summary = disclosure.locator("summary");
                    assertEquals("false", summary.getAttribute("aria-expanded"));
                    summary.click();
                    assertEquals("true", summary.getAttribute("aria-expanded"));
                    var rows = disclosure.locator("table.helm-evidence-table tbody tr");
                    assertTrue(rows.count() > 0, "opened observation exposes all captured fields");
                    if (fixture) {
                        assertEquals(3, rows.count());
                        assertEquals("ATTEMPT 1 · RESULT", rows.nth(0).locator("td").nth(0).innerText());
                        assertEquals("RECOVERED", rows.nth(0).locator("td").nth(1).innerText());
                        assertEquals("ATTEMPT 2 · RESULT", rows.nth(1).locator("td").nth(0).innerText());
                        assertEquals("INFO", rows.nth(1).locator("td").nth(1).innerText());
                        assertEquals("ATTEMPT 3 · RESULT", rows.nth(2).locator("td").nth(0).innerText());
                        assertEquals("EXHAUSTED", rows.nth(2).locator("td").nth(1).innerText());
                    } else {
                        assertTrue(disclosure.innerText().contains("ATTEMPT 1 · OPERATION"));
                        assertTrue(disclosure.innerText().contains("not captured"));
                    }
                    summary.press("Space");
                    assertFalse((Boolean) disclosure.evaluate("node => node.open"),
                            "each observation remains independently keyboard collapsible");
                }
                if (!fixture) {
                    String html = Files.readString(report);
                    assertFalse(html.contains("backend-private-body-must-not-be-captured"));
                    assertFalse(html.contains("127.0.0.1"));
                }
                assertTrue(errors.isEmpty(), () -> "Report runtime errors: " + errors);
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static Path publishFixture() throws Exception {
        Path reportPath = Files.createTempDirectory("atf-assertion-observation-browser-").resolve("observation.html");
        try (var report = new AssertionAuditReport(new ExtentReportSession(reportPath),
                new AssertionAuditReport.Identity("AssertionAuditFixture", "assertion audit",
                        "component observations", "verify disclosures", List.of("component")),
                new SensitiveDataRedactor())) {
            report.phase("TEST BODY", List.of(new AtfAssertion("Business result", "recovered", "recovered", true)),
                    AtfReportStatus.PASS, "", null, List.of(new ReportObservation("Backend recovery trace", List.of(
                            new ReportField("Attempt 1 · result", "RECOVERED"),
                            new ReportField("Attempt 2 · result", "INFO"),
                            new ReportField("Attempt 3 · result", "EXHAUSTED")), List.of(), List.of())));
            report.complete(AtfReportStatus.PASS);
        }
        return reportPath;
    }
}
