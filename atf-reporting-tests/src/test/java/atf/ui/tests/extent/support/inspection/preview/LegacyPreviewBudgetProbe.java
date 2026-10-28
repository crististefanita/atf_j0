package atf.ui.tests.extent.support.inspection.preview;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import atf.ui.tests.extent.support.locators.preview.ReportPreviewLocators;
import com.microsoft.playwright.*;

import java.nio.file.*;
import java.util.*;
import static atf.reporting.contract.AtfReportStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/** Direct reporting ports exercise the legacy UI path independently of Kit's component projection. */
public final class LegacyPreviewBudgetProbe {
    private static final String SECRET = "private-preview-credential";
    private static final String EXACT = "<>&\"".repeat(60);
    private static final ReportDisclosureLocators LOCATORS = new ReportDisclosureLocators();
    private static final ReportPreviewLocators PREVIEWS = new ReportPreviewLocators();
    private LegacyPreviewBudgetProbe() { }

    public static void verify(boolean fallback) throws Exception {
        verify(fallback, 240);
    }

    public static void verify(boolean fallback, int limit) throws Exception {
        Path report = publish(fallback, limit);
        String html = Files.readString(report);
        assertFalse(html.contains(SECRET), "full raw HTML must not retain a private prefix or hidden raw value");
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage()) {
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
            openTests(page, fallback);
            assertTableValues(page, limit);
            page.setViewportSize(392, 900);
            assertTableValues(page, limit);
            if (fallback) {
                PREVIEWS.fallbackFailsTab(page).click();
                var causes = PREVIEWS.fallbackCauses(page).allTextContents();
                assertFalse(causes.isEmpty());
                causes.forEach(cause -> assertTrue(cause.length() <= limit, () -> "fallback Fails value length=" + cause.length()));
                PREVIEWS.fallbackFailureLinks(page).first().click();
                LOCATORS.fallbackTestsTab(page).click();
            } else {
                PREVIEWS.failsTab(page).click();
                var items = PREVIEWS.failureItems(page);
                assertTrue(items.count() >= 3);
                for (int i = 0; i < items.count(); i++) {
                    items.nth(i).click();
                    String cause = PREVIEWS.failureCause(page).innerText();
                    assertTrue(cause.length() <= limit, () -> "Extent Fails value length=" + cause.length() + "; " + cause);
                }
                PREVIEWS.openFailureInTests(page).click();
            }
            openTests(page, fallback);
            assertTableValues(page, limit);
            assertTrue(errors.isEmpty(), errors::toString);
        }
    }

    private static Path publish(boolean fallback, int limit) throws Exception {
        Path report = TestReportPaths.report("legacy-preview-" + (fallback ? "fallback-" : "extent-")
                + limit + "-" + System.currentTimeMillis() + ".html");
        var settings = new ReportingSettings(limit, 18);
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(report, settings)
                : new ExtentReportSession(report, settings)) {
            var scenario = session.startScenario("RawUiPreview", "atf.reporting.preview",
                    "full values decide the verdict", List.of("e2e", "fixture"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("controlled raw UI failure", "", "");
            var validation = request.startValidation("Failure evidence");
            validation.evidenceOwner(ReportEvidenceOwner.UI);
            String prefix = "same prefix " + "x".repeat(400);
            String expected = prefix + "expected tail", actual = prefix + "actual tail";
            String crossing = "TimeoutError\n{\"token\":\"" + SECRET.repeat(40)
                    + "\",\"payload\":\"" + "x".repeat(350) + "\"}";
            String late = "late trace\n" + "safe ".repeat(80) + "\n{\"token\":\"" + SECRET + "\"}";
            String exact = EXACT.substring(0, limit);
            validation.assertions(List.of(new AtfAssertion("Exact boundary", exact, exact, true),
                    new AtfAssertion("Full values still decide", expected, actual, expected.equals(actual)),
                    new AtfAssertion("Failure cause", "no failure", crossing, false),
                    new AtfAssertion("Late multiline cause", "no failure", late, false)));
            validation.uiActions(List.of(new AtfUiActionRecord(1, "Click", "#submit_search", EXACT,
                            crossing, 1234, FAIL, AtfUiEvidenceSource.ACTION),
                    new AtfUiActionRecord(2, "Readback", "/read?token=" + SECRET, EXACT, crossing, 0,
                            FAIL, AtfUiEvidenceSource.OBSERVE),
                    new AtfUiActionRecord(3, "Late readback", "#result", "bounded observation", late,
                            0, FAIL, AtfUiEvidenceSource.OBSERVE)));
            validation.completeFailure(FAIL);
            request.complete(FAIL);
            phase.complete(FAIL);
            scenario.complete(FAIL);
        }
        return report;
    }

    private static void openTests(Page page, boolean fallback) {
        if (fallback) LOCATORS.fallbackTestsTab(page).click();
        else {
            LOCATORS.firstOwner(page).click();
            PREVIEWS.expandAllNodes(page).click();
        }
        for (var summary : PREVIEWS.summaries(page).all()) {
            if (summary.isVisible() && !(Boolean) summary.evaluate("s=>s.parentElement.open")) summary.press("Enter");
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertTableValues(Page page, int limit) {
        var values = (List<Map<String, String>>) page.evaluate("""
                () => Array.from(document.querySelectorAll('table')).flatMap(table => {
                  if(!table.tHead) return [];
                  const labels=Array.from(table.tHead.rows[0].cells,c=>c.textContent.trim().toUpperCase());
                  return Array.from(table.tBodies).flatMap(body=>Array.from(body.rows).flatMap(row=>
                    Array.from(row.cells).filter((cell,i)=>['EXPECTED','ACTUAL','OBSERVED'].includes(labels[i]))
                      .map(cell=>({value:cell.textContent,name:row.cells[0].textContent,result:row.cells[row.cells.length-1].textContent}))));
                })
                """);
        assertTrue(values.size() >= 12, () -> "fixture coverage=" + values.size());
        values.forEach(value -> assertTrue(value.get("value").length() <= limit,
                () -> value.get("name") + " value length=" + value.get("value").length()));
        assertTrue(values.stream().anyMatch(row -> row.get("name").equals("Exact boundary") && row.get("value").equals(EXACT.substring(0, limit))));
        var mismatch = values.stream().filter(row -> row.get("name").equals("Full values still decide")).toList();
        assertFalse(mismatch.isEmpty());
        assertTrue(mismatch.stream().allMatch(row -> row.get("result").contains("❌") || row.get("result").toUpperCase(Locale.ROOT).contains("FAIL")));
        assertEquals(1, mismatch.stream().map(row -> row.get("value")).distinct().count(),
                "equal previews must preserve the independently evaluated failed verdict");
    }
}
