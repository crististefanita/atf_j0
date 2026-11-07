package atf.ui.tests.extent.component.inspection.profile;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.artifact.resources.ReportArtifactResources;
import atf.reporting.support.composition.SelfContainedAuditFixture;
import atf.reporting.support.composition.SelfContainedAuditOracle;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.ArrayList;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** HTML-only detached acceptance with networking unavailable, one selected checkpoint and no bulk expansion. */
@AtfComponentTest
@ReportBrowserTest
class SelfContainedArtifactOfflineComponentTest {
    @TempDir Path directory;
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void detachedHtmlRetainsCompleteFactsComparisonsAndOriginalPng(boolean extent) throws Exception {
        Path original = SelfContainedAuditFixture.publish(extent, true, true, "offline");
        Path report = Files.copy(original, directory.resolve("detached-" + extent + ".html"));
        SelfContainedAuditOracle.verify(report, extent, true, true);
        assertTrue(ReportArtifactResources.inspect(Files.readString(report)).isAutonomous());
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value()));
             var contextLease = BrowserResources.open(() -> BrowserResources.context(browserLease.value(), new Browser.NewContextOptions().setOffline(true).setViewportSize(1100, 900)))) {
            try {
                var context = contextLease.value();
                var page = BrowserResources.newPage(context); page.setDefaultTimeout(5_000); page.setDefaultNavigationTimeout(30_000);
                var errors = new ArrayList<String>(); var external = new ArrayList<String>();
                page.onPageError(errors::add);
                page.onRequest(r -> { if (!r.url().startsWith("file:") && !r.url().startsWith("data:")) external.add(r.url()); });
                page.navigate(report.toUri() + (extent ? "#test-view" : ""));
                if (extent) page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                else page.locator("nav.tabs > button[data-tab=tests]").click();
                var link = page.locator((extent ? ".test-content-detail" : "#tests") + " a[data-atf-screenshot=base64]").first();
                var cards = link.locator("xpath=ancestor::*[contains(concat(' ',normalize-space(@class),' '),' card ')]");
                for (int index = 0; index < cards.count(); index++) {
                    var child = index + 1 < cards.count() ? cards.nth(index + 1) : link;
                    if (!child.isVisible()) cards.nth(index).locator(":scope > .card-header").click();
                }
                var details = link.locator("xpath=ancestor::details");
                for (int index = 0; index < details.count(); index++)
                    if (details.nth(index).getAttribute("open") == null) details.nth(index).locator(":scope > summary").click();
                assertTrue(link.isVisible());
                assertTrue(link.getAttribute("href").endsWith(SelfContainedAuditFixture.png(0)), "Original supplied PNG bytes");
                link.click();
                page.waitForFunction("() => Array.from(document.images).some(i => i.complete && i.naturalWidth === 2 && i.getBoundingClientRect().width > 0)");
                assertEquals(Boolean.TRUE, page.evaluate("() => Array.from(document.images).filter(i=>i.naturalWidth===2 && i.getBoundingClientRect().width>0).every(i=>{let r=i.getBoundingClientRect();return r.left>=0&&r.top>=0&&r.right<=innerWidth+1&&r.bottom<=innerHeight+1;})"));
                page.screenshot(new Page.ScreenshotOptions().setPath(directory.resolve("offline-viewer-" + extent + ".png")));
                page.keyboard().press("Escape"); assertTrue(link.isVisible(), "Return to original checkpoint");
                assertTrue(errors.isEmpty(), errors.toString()); assertTrue(external.isEmpty(), external.toString());
                System.out.println("DETACHED_HTML_REPORT=" + report);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}
