package atf.ui.tests.extent.support.inspection.failurecontext;

import atf.reporting.ui.failurecontext.UiFailureContextFixture;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import atf.ui.tests.extent.support.locators.preview.ReportPreviewLocators;
import atf.ui.tests.extent.support.locators.failurecontext.ReportFailureContextLocators;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class UiFailureContextProbe {
    private static final ReportDisclosureLocators COMMON = new ReportDisclosureLocators();
    private static final ReportPreviewLocators PREVIEW = new ReportPreviewLocators();
    private static final ReportFailureContextLocators CONTEXT = new ReportFailureContextLocators();
    private UiFailureContextProbe() { }

    public static void verify(boolean fallback, int limit) throws Exception {
        var report = UiFailureContextFixture.publish(fallback, limit);
        assertFalse(Files.readString(report).contains(UiFailureContextFixture.SECRET));
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage()) {
            var errors = new ArrayList<String>(); page.onPageError(errors::add);
            page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
            open(page, fallback);
            checkTables(page, limit);
            page.setViewportSize(392, 900);
            checkTables(page, limit);
            if (fallback) {
                PREVIEW.fallbackFailsTab(page).click();
                String cause = CONTEXT.fallbackCause(page).first().innerText();
                checkContext(cause, limit);
                PREVIEW.fallbackFailureLinks(page).first().click();
            } else {
                PREVIEW.failsTab(page).click(); PREVIEW.failureItems(page).first().click();
                checkContext(PREVIEW.failureCause(page).innerText(), limit);
                PREVIEW.openFailureInTests(page).click();
            }
            open(page, fallback); checkTables(page, limit);
            assertTrue(errors.isEmpty(), errors::toString);
        }
    }
    private static void open(Page page, boolean fallback) {
        if (fallback) COMMON.fallbackTestsTab(page).click();
        else { COMMON.firstOwner(page).click(); PREVIEW.expandAllNodes(page).click(); }
        for (var summary : PREVIEW.summaries(page).all())
            if (summary.isVisible() && !Boolean.TRUE.equals(summary.evaluate("s=>s.parentElement.open"))) summary.press("Enter");
    }
    private static void checkTables(Page page, int limit) {
        var assertion = CONTEXT.assertionRows(page).filter(new Locator.FilterOptions().setHasText("Failure cause")).first();
        checkContext(CONTEXT.cells(assertion).nth(2).innerText(), limit);
        var failed = CONTEXT.actionRows(page).filter(new Locator.FilterOptions().setHasText("#22")).first();
        checkContext(CONTEXT.cells(failed).nth(3).innerText(), limit);
        var observations = CONTEXT.observationRows(page);
        assertTrue(observations.count() >= 4, "primary, cause, suppressed and screenshot policy stay inspectable");
        assertTrue(observations.allTextContents().stream().anyMatch(text -> text.contains("Caused by diagnostic #1")));
        assertTrue(observations.allTextContents().stream().anyMatch(text -> text.contains("Suppressed by diagnostic #1")));
        for (var row : observations.all()) assertTrue(CONTEXT.cells(row).nth(3).innerText().length() <= limit);
        var disclosure = CONTEXT.observations(page).first();
        var summary = CONTEXT.summary(disclosure);
        summary.press("Enter"); assertFalse(Boolean.TRUE.equals(disclosure.evaluate("d=>d.open")));
        summary.press("Space"); assertTrue(Boolean.TRUE.equals(disclosure.evaluate("d=>d.open")));
    }
    private static void checkContext(String value, int limit) {
        assertTrue(value.length() <= limit, () -> "preview=" + value.length() + ": " + value);
        if (limit == 240) {
            assertTrue(value.contains("Click #22 -> #submit_search"), value);
            assertTrue(value.contains("elapsed=1234 ms"), value);
            assertTrue(value.contains("expected=results navigation completes"), value);
            assertFalse(value.contains("technical stack prefix"), value);
        }
    }
}
