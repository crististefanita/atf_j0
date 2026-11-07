package atf.ui.tests.extent.component.regression;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.support.regression.UnitBaselineReportFixture;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Behavioral oracles remain independent of selector spelling and passive performance listeners. */
@AtfComponentTest
@ReportBrowserTest
class UnitBaselineBehaviorComponentTest {
    @TempDir Path directory;

    @Test void fallbackTabsSupportClickKeyboardHashAndRejectAnUnboundTab() throws Exception {
        Path report = UnitBaselineReportFixture.publish(directory.resolve("fallback.html"), true);
        try (var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                prepare(page); page.navigate(report.toUri() + "#overview");
                active(page, "overview");
                for (String key : List.of("click", "Enter", "Space")) {
                    page.locator(".tab[data-tab=tests]").click(); active(page, "tests");
                    var tab = page.locator(".tab[data-tab=fails]");
                    if (key.equals("click")) tab.click(); else tab.press(key);
                    active(page, "fails");
                }
                page.evaluate("()=>location.hash='category'");
                page.waitForFunction("()=>document.querySelector('#category').classList.contains('active')"); active(page, "category");
                page.reload(); active(page, "category");
                page.screenshot(new Page.ScreenshotOptions().setPath(directory.resolve("fallback-navigation.png")));
                String source = Files.readString(report);
                String mutant = source.replace("tabs=nodes('.tab')", "tabs=nodes('.unbound-tab')");
                assertNotEquals(source, mutant, "The negative control must alter the published binding");
                Path broken = directory.resolve("unbound-tab.html"); Files.writeString(broken, mutant);
                page.navigate(broken.toUri() + "#overview"); page.locator(".tab[data-tab=tests]").click();
                assertThrows(AssertionError.class, () -> active(page, "tests"), "An actual unbound control must fail the same oracle");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    @Test void passiveLoadMeasurementDoesNotRestartNavigationAndDuplicateInitializationIsRejected() throws Exception {
        Path report = UnitBaselineReportFixture.publish(directory.resolve("extent-bootstrap.html"), false);
        try (var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                prepare(page);
                page.addInitScript("""
                        window.__baselineActivations=[];document.addEventListener('DOMContentLoaded',()=>{
                          const activate=window.atfActivate;window.atfActivate=function(id){
                            window.__baselineActivations.push(id);return activate.apply(this,arguments);};});
                        """);
                page.navigate(report.toUri() + "#test-view"); ready(page); initializedOnce(page);
                page.evaluate("()=>window.dispatchEvent(new Event('load'))"); settle(page); initializedOnce(page);
                String source = Files.readString(report);
                String mutant = source.replace("if(atfReportNavigationInitialized)return;", "");
                assertNotEquals(source, mutant, "The negative control must remove the real idempotence guard");
                Path broken = directory.resolve("duplicate-initialization.html"); Files.writeString(broken, mutant);
                page.navigate(broken.toUri() + "#test-view"); ready(page);
                page.evaluate("()=>window.dispatchEvent(new Event('load'))"); settle(page);
                assertThrows(AssertionError.class, () -> initializedOnce(page));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    @Test void failsKeepsFailedBodyRequestAndItsMeasuredDurationInsteadOfPassingSetup() throws Exception {
        Path report = UnitBaselineReportFixture.publish(directory.resolve("extent-ownership.html"), false);
        try (var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                prepare(page); var errors = new ArrayList<String>(); page.onPageError(errors::add);
                page.navigate(report.toUri() + "#atf-fails-view");
                page.waitForSelector(".atf-failure-index:visible"); nativeTestSelection(page); ownership(page);
                page.locator("#atf-failure-detail").evaluate("e=>e.className='detail-body'");
                nativeTestSelection(page);
                assertThrows(AssertionError.class, () -> ownership(page), "A native mutable host must fail the same ownership oracle");
                page.reload(); page.waitForSelector(".atf-failure-index:visible"); ownership(page);
                var request = field(page, "REQUEST"); request.evaluate("e=>e.textContent='GET /setup-decoy'");
                assertThrows(AssertionError.class, () -> ownership(page)); request.evaluate("e=>e.textContent='GET /audit-target'");
                var duration = field(page, "DURATION"); duration.evaluate("e=>e.textContent='999 ms'");
                assertThrows(AssertionError.class, () -> ownership(page));
                page.reload(); page.waitForSelector(".atf-failure-index:visible"); ownership(page);
                page.screenshot(new Page.ScreenshotOptions().setPath(directory.resolve("extent-failed-request.png")));
                page.locator(".atf-failure-open:visible").click();
                page.waitForSelector("#test-view table:visible");
                assertTrue(page.url().contains("failure-ordinal=1"));
                assertTrue(errors.isEmpty(), errors::toString);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void ownership(Page page) {
        assertEquals(1, page.locator(".atf-failure-index:visible").count(), "The diagnostic index must survive native Test selection");
        assertEquals("TEST BODY", field(page, "PHASE").innerText());
        assertEquals("GET /audit-target", field(page, "REQUEST").innerText());
        assertEquals("12 ms", field(page, "DURATION").innerText());
        assertEquals("HTTP status → 201 Created → 200 OK", field(page, "ASSERTION").innerText());
    }
    private static void nativeTestSelection(Page page) {
        page.evaluate("()=>window.jQuery('#test-view .test-list-item>li.test-item').first().triggerHandler('click')");
        settle(page);
    }
    private static Locator field(Page page, String label) {
        return page.locator(".atf-failure-index:visible .atf-failure-index-row")
                .filter(new Locator.FilterOptions().setHas(page.locator(".atf-failure-label").getByText(label,
                        new Locator.GetByTextOptions().setExact(true)))).locator(".atf-failure-value");
    }
    private static void initializedOnce(Page page) {
        assertEquals(List.of("test-view"), page.evaluate("()=>window.__baselineActivations"));
    }
    private static void active(Page page, String id) {
        assertEquals(id, page.locator(".tab-panel.active").getAttribute("id"));
        assertEquals(id, page.locator(".tab.active").getAttribute("data-tab"));
        assertTrue(page.locator("#" + id).isVisible()); assertTrue(page.url().endsWith("#" + id));
    }
    private static void ready(Page page) {
        page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'"); settle(page);
    }
    private static void settle(Page page) {
        page.evaluate("()=>new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done)))");
    }
    private static void prepare(Page page) {
        page.setDefaultTimeout(10000); page.setDefaultNavigationTimeout(45000);
        page.route("http://**/*", Route::abort); page.route("https://**/*", Route::abort);
    }
}
