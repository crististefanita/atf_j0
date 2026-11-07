package atf.ui.tests.extent.support.inspection.failurenavigation;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.support.failurecorrelation.FailureNavigationFixture;
import atf.reporting.support.failurecorrelation.FailureNavigationFixture.*;
import atf.ui.tests.extent.support.locators.failurenavigation.FailureNavigationLocators;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Oracles observe original rows before any assisted expansion and reject hidden ancestor regressions. */
public final class FailureNavigationEdgeProbe {
    private static final FailureNavigationLocators LOCATORS = new FailureNavigationLocators();
    private FailureNavigationEdgeProbe() { }

    public static void verifyRepeatedUiFailures(int limit, int width) throws Exception {
        Path report = FailureNavigationFixture.publishRepeatedUiFailures(limit);
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value(), new Browser.NewContextOptions().setViewportSize(width, 900)))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(4000);
                page.navigate(report.toUri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                var causes = java.util.List.of("primary failure", "cleanup refused");
                for (int index : new int[]{1, 0, 1}) {
                    LOCATORS.fails(page).click();
                    assertEquals(2, LOCATORS.failures(page).count(), "Derived summaries never create extra failure causes");
                    LOCATORS.failures(page).nth(index).click();
                    assertEquals(Integer.toString(index + 1), LOCATORS.backlink(page).getAttribute("data-atf-failure-ordinal"));
                    LOCATORS.backlink(page).click();
                    var row = LOCATORS.originalRepeatedFailure(page, index);
                    assertThat(row).isVisible();
                    assertEquals(causes.get(index), LOCATORS.cells(row).nth(2).innerText());
                    page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    assertThat(row).isVisible();
                    assertEquals(causes.get(index), LOCATORS.cells(row).nth(2).innerText());
                    assertThat(LOCATORS.sibling(page)).hasAttribute("data-atf-scenario-open", "false");
                    assertThat(LOCATORS.siblingBody(page)).not().isVisible();
                }
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    public static void verifyFallback(Owner owner, int width, boolean mixed) throws Exception {
        Path report = FailureNavigationFixture.publish(owner, true, mixed ? Evidence.MIXED : Evidence.ASSERTIONS, 240);
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value(), new Browser.NewContextOptions().setViewportSize(width, 900)))) {
            try {
                var page = pageLease.value();
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
                    waitForFallbackTarget(page, target, report, owner, width, phase, "backlink");
                    assertComparison(target);
                    target.evaluate("row=>row.closest('[data-atf-node-role]').style.display='none'");
                    assertThrows(AssertionError.class, () -> assertComparison(target), "A collapsed original validation must fail the oracle");
                    page.reload(); waitForFallbackTarget(page, target, report, owner, width, phase, "reload"); assertComparison(target);
                }
                assertTrue(errors.isEmpty(), errors::toString);
                page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(report + "-verified.png")));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    public static void verifyDiagnostics(boolean fallback, int limit) throws Exception {
        Path report = FailureNavigationFixture.publish(Owner.COMPONENT, fallback, Evidence.DIAGNOSTICS, limit);
        assertFalse(Files.readString(report).contains(FailureNavigationFixture.SECRET));
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value(), new Browser.NewContextOptions().setViewportSize(392, 900)))) {
            try {
                var page = pageLease.value();
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
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static boolean inViewport(Locator row) {
        return (Boolean) row.evaluateAll("""
                rows => {
                  if (rows.length !== 1) return false;
                  const bounds = rows[0].getBoundingClientRect();
                  return bounds.height > innerHeight ? bounds.top < innerHeight && bounds.bottom > 0
                      : bounds.height > 0 && bounds.top >= 0 && bounds.bottom <= innerHeight;
                }
                """);
    }

    private static void waitForFallbackTarget(Page page, Locator row, Path report, Owner owner,
                                               int width, String phase, String operation) {
        try {
            page.waitForCondition(() -> row.isVisible() && inViewport(row));
        } catch (PlaywrightException failure) {
            String geometry = row.evaluateAll("rows=>rows.map(row=>{const r=row.getBoundingClientRect();return JSON.stringify({top:r.top,bottom:r.bottom,"
                    + "height:r.height,width:r.width,viewport:[innerWidth,innerHeight],scroll:[scrollX,scrollY],"
                    + "ancestors:Array.from(document.querySelectorAll('details')).filter(d=>d.contains(row)).map(d=>[d.className,d.open])})})").toString();
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(report + "-viewport-timeout.png")));
            throw new AssertionError("Fallback target: " + owner + "/" + width + "/" + phase + "/" + operation
                    + "; report=" + report + "; geometry=" + geometry, failure);
        }
    }

    private static void assertComparison(Locator row) {
        assertTrue(row.isVisible(), "The original failed comparison is visible without expanding the report in the harness");
        assertEquals(4, LOCATORS.cells(row).count());
        assertEquals("Fail", LOCATORS.cells(row).nth(3).innerText().trim(), "Fallback preserves its original failure badge");
        assertEquals(1, LOCATORS.failedStatus(row).count());
        assertTrue(inViewport(row),
                () -> "Navigation scrolls the exact original row into the viewport: "
                        + row.evaluateAll("rows=>rows.map(row=>{const r=row.getBoundingClientRect();return [r.top,r.bottom,r.height,innerHeight]})"));
    }
}
