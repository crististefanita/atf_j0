package atf.ui.tests.extent.support.inspection.failurecontext;

import atf.reporting.ui.failurecontext.UiTimeoutReportFixture;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import atf.ui.tests.extent.support.locators.failurecontext.ReportFailureContextLocators;
import atf.ui.tests.extent.support.locators.preview.ReportPreviewLocators;
import atf.ui.tests.extent.support.locators.verdict.VerdictReportLocators;
import com.microsoft.playwright.*;

import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Browser oracle for each published surface; no renderer-derived expected formatting. */
public final class UiTimeoutReportProbe {
    private static final ReportFailureContextLocators CONTEXT = new ReportFailureContextLocators();
    private static final ReportDisclosureLocators COMMON = new ReportDisclosureLocators();
    private static final ReportPreviewLocators PREVIEW = new ReportPreviewLocators();
    private static final VerdictReportLocators VIEWS = new VerdictReportLocators();
    private UiTimeoutReportProbe() { }

    public static void verify(boolean fallback, int limit) throws Exception {
        var file = UiTimeoutReportFixture.publish(fallback, limit);
        String html = Files.readString(file);
        for (String secret : UiTimeoutReportFixture.SECRETS) assertFalse(html.contains(secret), "raw secret in artifact");
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage()) {
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(file.toUri().toString());
            VIEWS.tests(page, fallback).click();
            if (!fallback) { COMMON.firstOwner(page).click(); VIEWS.expandAll(page).click(); }
            var scenario = VIEWS.scenario(page, fallback, UiTimeoutReportFixture.SCENARIO);
            var diagnostics = CONTEXT.allObservations(scenario);
            assertEquals(1, diagnostics.count());
            assertFalse(open(diagnostics), "diagnostic arrives collapsed");
            assertFalse(open(CONTEXT.actionDetails(scenario)), "actions arrive collapsed");
            if (!fallback) assertFalse(open(CONTEXT.assertionDetails(scenario)), "Extent validation assertions arrive collapsed");
            for (var summary : PREVIEW.summaries(page).all())
                if (summary.isVisible() && !Boolean.TRUE.equals(summary.evaluate("s=>s.parentElement.open"))) summary.press("Enter");
            checkTests(page, fallback, limit);
            CONTEXT.summary(diagnostics).press("Enter"); assertFalse(open(diagnostics));
            CONTEXT.summary(diagnostics).press("Space"); assertTrue(open(diagnostics));
            checkTests(page, fallback, limit);
            proveOracleRejectsMissingTimeoutAndRawCredential(scenario, limit);
            page.setViewportSize(392, 900);
            checkTests(page, fallback, limit);
            VIEWS.fails(page, fallback).click();
            assertEquals(1, VIEWS.failures(page, fallback).count(), "only the timeout scenario fails");
            if (!fallback) VIEWS.failures(page, false).first().click();
            String cause = (fallback ? CONTEXT.fallbackCause(page) : PREVIEW.failureCause(page)).innerText();
            checkContext(cause, limit);
            VIEWS.openFailure(page, fallback).click();
            assertThat(VIEWS.scenario(page, fallback, UiTimeoutReportFixture.SCENARIO)).isVisible();
            if (!fallback) VIEWS.expandAll(page).click();
            for (var summary : PREVIEW.summaries(page).all())
                if (summary.isVisible() && !Boolean.TRUE.equals(summary.evaluate("s=>s.parentElement.open"))) summary.press("Enter");
            checkTests(page, fallback, limit);
            VIEWS.overview(page, fallback).click();
            assertEquals("1", VIEWS.metric(page, fallback, fallback ? "Failed" : "Tests Failed").innerText());
            assertEquals("1", VIEWS.metric(page, fallback, fallback ? "Passed" : "Tests Passed").innerText());
            assertTrue(errors.isEmpty(), errors::toString);
        }
    }

    private static void checkTests(Page page, boolean fallback, int limit) {
        var scenario = VIEWS.scenario(page, fallback, UiTimeoutReportFixture.SCENARIO);
        assertEquals(0, CONTEXT.httpDetails(scenario).count(), "UI timeout does not invent HTTP evidence");
        var action = CONTEXT.actionRows(page).first();
        var cells = CONTEXT.cells(action);
        assertEquals("Wait for visible", CONTEXT.operation(action).innerText());
        assertEquals("#1", CONTEXT.sequence(action).innerText());
        assertEquals(UiTimeoutReportFixture.TARGET, cells.nth(1).innerText());
        checkContext(cells.nth(3).innerText(), limit);
        assertEquals("137 ms", cells.nth(4).innerText(), "measured time is independent of the native timeout deadline");
        assertEquals("Fail", CONTEXT.result(action).getAttribute("aria-label"));
        var assertion = CONTEXT.assertionRows(page).filter(new Locator.FilterOptions().setHasText("Failure cause")).first();
        checkContext(CONTEXT.cells(assertion).nth(2).innerText(), limit);
        checkDiagnostics(scenario, limit);
        var rows = CONTEXT.observationRows(page);
        assertEquals(5, rows.count(), "primary, cause, suppressed, original action and screenshot policy");
        rows.last().scrollIntoViewIfNeeded();
        assertThat(rows.last()).isVisible();
        var sibling = VIEWS.scenario(page, fallback, "unrelated passing scenario");
        assertEquals(0, CONTEXT.allObservations(sibling).count(), "timeout stays in its owner");
    }

    private static void checkDiagnostics(Locator scenario, int limit) {
        String[][] expected = {
                {"Primary exception", "TimeoutException: Timeout 20000ms exceeded; password=***REDACTED***; Authorization: Bearer ***REDACTED***"},
                {"Caused by diagnostic #1", "IllegalStateException: timeout cause; token=***REDACTED***"},
                {"Suppressed by diagnostic #1", "IllegalArgumentException: timeout cleanup; password=***REDACTED***"},
                {"Recorded action #1", "Timeout 20000ms exceeded"}
        };
        for (String[] diagnostic : expected) {
            var row = CONTEXT.diagnostic(scenario, diagnostic[0]);
            assertEquals(1, row.count(), diagnostic[0]);
            assertEquals("Info", CONTEXT.result(row).getAttribute("aria-label"), "diagnostics retain their informative role");
            String value = CONTEXT.cells(row).nth(3).innerText();
            assertEquals(limit == 240 ? diagnostic[1] : diagnostic[1].substring(0, limit - 1) + "…", value);
            for (String secret : UiTimeoutReportFixture.SECRETS) assertFalse(value.contains(secret));
        }
    }

    private static void proveOracleRejectsMissingTimeoutAndRawCredential(Locator scenario, int limit) {
        var cell = CONTEXT.cells(CONTEXT.diagnostic(scenario, "Primary exception")).nth(3);
        String original = cell.innerText();
        try {
            cell.evaluate("e => e.textContent = 'diagnostic removed'");
            assertThrows(AssertionError.class, () -> checkDiagnostics(scenario, limit));
            cell.evaluate("e => e.textContent = 'password=raw-timeout-password'");
            assertThrows(AssertionError.class, () -> checkDiagnostics(scenario, limit));
        } finally { cell.evaluate("(e, text) => e.textContent = text", original); }
    }

    private static boolean open(Locator disclosure) { return Boolean.TRUE.equals(disclosure.evaluate("d=>d.open")); }
    private static void checkContext(String value, int limit) {
        assertTrue(value.length() <= limit, value);
        if (limit == 240) assertEquals("TimeoutException; first failed evidence: Wait for visible #1 -> "
                + UiTimeoutReportFixture.TARGET + "; elapsed=137 ms; expected=login submit becomes visible", value);
        else assertEquals("TimeoutException;…", value);
    }
}
