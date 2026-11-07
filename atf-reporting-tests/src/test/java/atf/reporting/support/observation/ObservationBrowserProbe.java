package atf.reporting.support.observation;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.contract.*;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.*;
import atf.reporting.support.TestReportPaths;
import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Extent/fallback files, two different invocations, nested keyboard and overflow evidence. */
public final class ObservationBrowserProbe {
    private ObservationBrowserProbe() { }
    public static void verify() {
        try(var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)))) {
            try {
                Playwright playwright = playwrightLease.value();
                Browser browser = browserLease.value();
                for(boolean extent:List.of(true,false)) {
                    Path file=fixture(extent);Page page=BrowserResources.newPage(browser);var errors=new ArrayList<String>();page.onPageError(errors::add);
                    page.navigate(file.toUri()+"#test-view");
                    if(extent){
                        page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                        var scenario=page.locator(".test-content-detail .test-contents > .mt-4 > .accordion > .card").first();
                        scenario.locator(":scope > .card-header").click();
                        var phase=scenario.locator(":scope > .accordion > .card").first();
                        phase.locator(":scope > .card-header").click();
                        phase.locator(":scope > .accordion > .card").first().locator(":scope > .card-header").click();
                    }
                    else page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Tests").setExact(true)).click();
                    Locator parent=page.locator("[data-atf-node-role='observation'] > details:visible").first();
                    Locator summary=parent.locator(":scope > summary");summary.click();
                    Locator child=parent.locator("details").first(),childSummary=child.locator(":scope > summary");
                    assertNull(child.getAttribute("open"));
                    if(extent)assertEquals("＋",childSummary.locator(".atf-disclosure-glyph").evaluate("e => getComputedStyle(e, '::before').content.replaceAll(String.fromCharCode(34), '')"));
                    childSummary.press("Enter");assertNotNull(child.getAttribute("open"));
                    if(extent)assertEquals("−",childSummary.locator(".atf-disclosure-glyph").evaluate("e => getComputedStyle(e, '::before').content.replaceAll(String.fromCharCode(34), '')"));
                    assertTrue(child.innerText().contains("FINAL-ROW-alpha"));
                    assertEquals("normal",child.locator("pre").evaluate("e => getComputedStyle(e).wordBreak"));
                    child.locator("pre").press("End");
                    page.waitForFunction("e => e.scrollTop + e.clientHeight >= e.scrollHeight - 1",child.locator("pre").elementHandle());
                    assertEquals(true,child.locator("pre").evaluate("e => e.scrollTop + e.clientHeight >= e.scrollHeight - 1"));
                    childSummary.press(" ");assertNull(child.getAttribute("open"));
                    if(extent)assertEquals("＋",childSummary.locator(".atf-disclosure-glyph").evaluate("e => getComputedStyle(e, '::before').content.replaceAll(String.fromCharCode(34), '')"));
                    if(extent)page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Expand all nodes")).click();
                    assertTrue(page.content().contains("FINAL-ROW-beta"));assertTrue(errors.isEmpty(),errors.toString());BrowserResources.close(page);
                }
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static Path fixture(boolean extent) {
        Path file=TestReportPaths.report(extent?"observations-extent.html":"observations-fallback.html");
        var settings=new ReportingSettings(240,12000);
        try(AtfReportPorts.Session session=extent?new ExtentReportSession(file,settings):new HtmlReportSession(file,settings)) {
            for(String id:List.of("alpha","beta"))try(var scenario=session.startScenario("ObservationBrowser",id,List.of("component"));var phase=scenario.startPhase("TEST BODY")) {
                phase.observation(new ReportObservation("Observation "+id,List.of(new ReportField("Run",id),new ReportField("Preview","x".repeat(350))),
                        List.of(new ReportObservation.Body("Complete transcript "+id,"spoken word ".repeat(200)+"FINAL-ROW-"+id,ReportObservation.Format.TEXT)),List.of()));
                phase.complete(AtfReportStatus.INFO);scenario.complete(AtfReportStatus.INFO);
            }
        }
        return file;
    }
}
