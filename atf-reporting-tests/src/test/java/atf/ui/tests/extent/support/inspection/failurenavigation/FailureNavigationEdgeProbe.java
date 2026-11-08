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
                if (fallback) verifyFallbackDiagnostics(page, limit);
                else verifyExtentDiagnostics(page, limit);
                assertTrue(errors.isEmpty(), errors::toString);
                page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(report + "-verified.png")));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void verifyExtentDiagnostics(Page page, int limit) {
        LOCATORS.fails(page).click();
        var failures = LOCATORS.failures(page);
        assertEquals(1, failures.count(), "Extent indexes one ordered diagnostic chain per scenario");
        var aggregate = failures.first();
        assertEquals("diagnostic", aggregate.getAttribute("data-atf-failure-kind"));
        assertEquals("SETUP", aggregate.getAttribute("data-atf-failure-phase"));
        aggregate.click();
        assertTrue(LOCATORS.cause(page).innerText().length() <= limit,
                "The aggregate cause respects the configured preview");
        var backlink = LOCATORS.backlink(page);
        assertEquals("diagnostic", backlink.getAttribute("data-atf-failure-kind"));
        assertEquals("1", backlink.getAttribute("data-atf-failure-ordinal"));
        backlink.click();
        var setupDiagnostic = LOCATORS.diagnostic(page, false, "SETUP");
        page.waitForCondition(setupDiagnostic::isVisible);
        assertEquals(0, LOCATORS.assertionRows(page, false).count(),
                "Execution diagnostics never manufacture contract comparisons");

        var scenario = LOCATORS.extentScenario(page);
        assertTrue(scenario.isVisible());
        var disclosures = LOCATORS.diagnosticDisclosures(scenario);
        assertEquals(FailureNavigationFixture.PHASES.size(), disclosures.count(),
                "Tests retains one independently collapsible disclosure for each source phase");
        for (int index = 0; index < FailureNavigationFixture.PHASES.size(); index++) {
            String phase = FailureNavigationFixture.PHASES.get(index);
            var disclosure = disclosures.nth(index);
            var phaseBody = disclosure.locator("[data-atf-phase-name]");
            assertEquals(phase, phaseBody.getAttribute("data-atf-phase-name"));
            var summary = LOCATORS.diagnosticSummary(disclosure);
            if (Boolean.TRUE.equals(disclosure.evaluate("node => node.open"))) summary.click();
            assertEquals(Boolean.FALSE, disclosure.evaluate("node => node.open"),
                    "Each diagnostic is independently collapsible");
            summary.click();
            assertEquals(Boolean.TRUE, disclosure.evaluate("node => node.open"));
            var diagnostic = LOCATORS.diagnostic(disclosure, phase);
            assertEquals(1, diagnostic.count());
            assertTrue(diagnostic.isVisible());
            assertDiagnosticRow(page, diagnostic, phase, false);
            summary.click();
            assertEquals(Boolean.FALSE, disclosure.evaluate("node => node.open"));
        }
    }

    private static void verifyFallbackDiagnostics(Page page, int limit) {
        LOCATORS.fallbackFails(page).click();
        var failures = LOCATORS.fallbackFailures(page);
        assertEquals(FailureNavigationFixture.PHASES.size(), failures.count(),
                "Fallback keeps one independently navigable diagnostic row per phase");
        for (int index = 0; index < FailureNavigationFixture.PHASES.size(); index++) {
            String phase = FailureNavigationFixture.PHASES.get(index);
            LOCATORS.fallbackFails(page).click();
            assertEquals(FailureNavigationFixture.PHASES.size(), failures.count());
            var link = failures.nth(index);
            assertTrue(LOCATORS.fallbackFailurePreview(link).innerText().length() <= limit,
                    "The fallback Fails preview respects the configured character budget");
            link.click();
            var diagnostic = LOCATORS.diagnostic(page, true, phase);
            page.waitForCondition(diagnostic::isVisible);
            assertDiagnosticRow(page, diagnostic, phase, true);
            page.reload();
            page.waitForCondition(diagnostic::isVisible);
            assertDiagnosticRow(page, diagnostic, phase, true);
        }
    }

    private static void assertDiagnosticRow(Page page, Locator diagnostic, String phase,
                                           boolean fallback) {
        assertEquals(2, LOCATORS.cells(diagnostic).count(), "A diagnostic keeps its typed message pair");
        assertEquals(0, LOCATORS.assertionRows(page, fallback).count(),
                "Execution diagnostics never manufacture contract comparisons");
        assertTrue(LOCATORS.cells(diagnostic).nth(1).innerText().contains("phase=" + phase));
        assertFalse(diagnostic.innerText().contains(FailureNavigationFixture.SECRET));
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
