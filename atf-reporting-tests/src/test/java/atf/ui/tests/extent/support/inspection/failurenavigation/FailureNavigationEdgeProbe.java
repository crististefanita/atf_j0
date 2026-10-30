package atf.ui.tests.extent.support.inspection.failurenavigation;

import atf.reporting.support.failurecorrelation.FailureNavigationFixture;
import atf.reporting.support.failurecorrelation.FailureNavigationFixture.*;
import atf.ui.tests.extent.support.locators.failurenavigation.FailureNavigationLocators;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/** Oracles observe original rows before any assisted expansion and reject hidden ancestor regressions. */
public final class FailureNavigationEdgeProbe {
    private static final FailureNavigationLocators LOCATORS = new FailureNavigationLocators();
    private FailureNavigationEdgeProbe() { }

    public static void verifyFallback(Owner owner, int width, boolean mixed) throws Exception {
        Path report = FailureNavigationFixture.publish(owner, true, mixed ? Evidence.MIXED : Evidence.ASSERTIONS, 240);
        try (var playwright = Playwright.create(); var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900))) {
            page.setDefaultTimeout(4000);
            var errors = new ArrayList<String>(); page.onPageError(errors::add);
            page.navigate(report.toUri().toString());
            for (int index = 0; index < FailureNavigationFixture.PHASES.size(); index++) {
                String phase = FailureNavigationFixture.PHASES.get(index);
                int ordinal = (index + 1) * (mixed ? 3 : 2);
                LOCATORS.fallbackFails(page).click();
                assertEquals(mixed ? 9 : 6, LOCATORS.fallbackFailures(page).count());
                var link = LOCATORS.fallbackFailures(page).nth(ordinal - 1);
                assertEquals(Integer.toString(ordinal), link.getAttribute("data-atf-failure-ordinal"));
                link.press("Enter");
                var target = LOCATORS.fallbackTarget(page, phase);
                page.waitForCondition(() -> target.isVisible() && inViewport(target));
                assertComparison(target);
                target.evaluate("row=>row.closest('[data-atf-node-role]').style.display='none'");
                assertThrows(AssertionError.class, () -> assertComparison(target), "A collapsed original validation must fail the oracle");
                page.reload(); page.waitForCondition(() -> target.isVisible() && inViewport(target)); assertComparison(target);
            }
            assertTrue(errors.isEmpty(), errors::toString);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(report + "-verified.png")));
        }
    }

    public static void verifyDiagnostics(boolean fallback, int limit) throws Exception {
        Path report = FailureNavigationFixture.publish(Owner.COMPONENT, fallback, Evidence.DIAGNOSTICS, limit);
        assertFalse(Files.readString(report).contains(FailureNavigationFixture.SECRET));
        try (var playwright = Playwright.create(); var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(392, 900))) {
            page.setDefaultTimeout(4000);
            var errors = new ArrayList<String>(); page.onPageError(errors::add);
            page.navigate(report.toUri().toString());
            for (int index = 0; index < FailureNavigationFixture.PHASES.size(); index++) {
                String phase = FailureNavigationFixture.PHASES.get(index);
                if (fallback) LOCATORS.fallbackFails(page).click(); else LOCATORS.fails(page).click();
                var failures = fallback ? LOCATORS.fallbackFailures(page) : LOCATORS.failures(page);
                assertEquals(3, failures.count(), "Every failed phase retains its own real execution diagnostic");
                var link = failures.nth(index);
                if (!fallback) {
                    link.click(); link = LOCATORS.backlink(page);
                    assertEquals("diagnostic", link.getAttribute("data-atf-failure-kind"));
                    assertTrue(LOCATORS.cause(page).innerText().length() <= limit, "The selected diagnostic's cause respects the configured preview");
                }
                link.click();
                var diagnostic = LOCATORS.diagnostic(page, fallback, phase);
                page.waitForCondition(diagnostic::isVisible);
                assertEquals(0, LOCATORS.assertionRows(page, fallback).count(), "Execution failure never manufactures a contract comparison");
                assertTrue(LOCATORS.cells(diagnostic).nth(1).innerText().contains("phase=" + phase));
                page.reload(); page.waitForCondition(diagnostic::isVisible);
            }
            assertTrue(errors.isEmpty(), errors::toString);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(report + "-verified.png")));
        }
    }

    private static boolean inViewport(Locator row) {
        return (Boolean) row.evaluate("row=>{const r=row.getBoundingClientRect();return r.height>innerHeight?r.top<innerHeight&&r.bottom>0:r.top>=0&&r.bottom<=innerHeight}");
    }

    private static void assertComparison(Locator row) {
        assertTrue(row.isVisible(), "The original failed comparison is visible without expanding the report in the harness");
        assertEquals(4, LOCATORS.cells(row).count());
        assertEquals("Fail", LOCATORS.cells(row).nth(3).innerText().trim(), "Fallback preserves its original failure badge");
        assertEquals(1, LOCATORS.failedStatus(row).count());
        assertTrue(inViewport(row),
                () -> "Navigation scrolls the exact original row into the viewport: "
                        + row.evaluate("row=>{const r=row.getBoundingClientRect();return [r.top,r.bottom,r.height,innerHeight]}"));
    }
}
