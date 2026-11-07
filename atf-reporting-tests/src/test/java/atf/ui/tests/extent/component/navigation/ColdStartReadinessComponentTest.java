package atf.ui.tests.extent.component.navigation;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.testkit.AtfComponentTest;
import atf.reporting.support.navigation.LargeNavigationFixture;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Startup reaches the requested surface once; readiness cannot certify a failed normalization. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ReportBrowserTest
class ColdStartReadinessComponentTest {
    Path fixture;
    @BeforeAll void publish() throws Exception {fixture=LargeNavigationFixture.publish();}
    @ParameterizedTest
    @ValueSource(strings={"test-view","category-view","atf-fails-view","dashboard-view",""})
    void initializesOnlyTheRequestedViewAndKeepsReadinessStable(String hash) throws Exception {
        try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                prepare(page);long began=System.nanoTime();page.navigate(fixture.toUri()+"#"+hash);settle(page);
                String expected=hash.isEmpty()?"dashboard-view":hash;
                assertEquals(List.of(expected),page.evaluate("()=>window.__coldActivations"),"Bootstrap must not activate an unrelated or duplicate view");
                assertThat(page.locator("#"+expected)).isVisible();
                assertEquals(page.evaluate("()=>performance.getEntriesByType('navigation')[0].duration"),page.evaluate("()=>window.__atfReportPerformance.pageLoadMs"));
                if(expected.equals("test-view"))page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");
                assertNavigationTargets(page);
                page.locator("#nav-fails").evaluate("e=>e.setAttribute('href','#')");
                Object stamp=page.evaluate("()=>window.__atfReportPerformance.readinessStartedAt");
                page.evaluate("()=>window.dispatchEvent(new Event('load'))");settle(page);
                assertEquals(List.of(expected),page.evaluate("()=>window.__coldActivations"),"A repeated load event must not restart the active view");
                assertNavigationTargets(page);
                assertEquals(stamp,page.evaluate("()=>window.__atfReportPerformance.readinessStartedAt"));
                System.out.println("COLD_BOOT="+expected+";elapsedMs="+(System.nanoTime()-began)/1000000);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    @Test void freshBrowserProcessesBindRealControlsWithinTheOriginalDeadline() throws Exception {
        String input=System.getProperty("atf.cold.input");Path file=input==null?fixture:Path.of(input);
        int runs=input==null?1:3;
        for(int run=0;run<runs;run++)try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                prepare(page);long begin=System.nanoTime();page.navigate(file.toUri()+"#test-view");
                long loaded=System.nanoTime();
                page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");long ready=System.nanoTime();settle(page);
                assertEquals(List.of("test-view"),page.evaluate("()=>window.__coldActivations"));
                var owner=page.locator("#test-view .test-list-wrapper .test-list-item>li.test-item[test-id]").last();
                String expected=owner.locator(".test-detail .name").innerText();owner.locator(".test-detail").click();
                assertThat(page.locator("#test-view .detail-head h5:visible")).hasText(expected);
                var header=page.locator("#test-view .test-content-detail .test-contents>.mt-4>.accordion>.card>.card-header").first();
                assertThat(header).hasAttribute("role","button");header.press("Enter");assertThat(header).hasAttribute("aria-expanded","true");
                System.out.println("COLD_FRESH_READY="+run+";navigationMs="+(loaded-begin)/1000000+";readyWaitMs="+(ready-loaded)/1000000
                    +";throughFirstActionMs="+(System.nanoTime()-begin)/1000000+";metrics="+page.evaluate("()=>window.__atfReportPerformance"));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    @Test void preservesStartupDeepLinksWithoutASecondActivation() throws Exception {
        var owner=org.jsoup.Jsoup.parse(Files.readString(fixture)).selectFirst("#test-view .test-list-item>li.test-item[test-id]");
        String id=Objects.requireNonNull(owner).attr("test-id"),name=Objects.requireNonNull(owner.selectFirst(".test-detail .name")).text();
        try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                prepare(page);page.navigate(fixture.toUri()+"#test-id="+id+"&scenario-index=0");
                page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");settle(page);
                assertThat(page.locator("#test-view .detail-head h5:visible")).hasText(name);
                assertThat(page.locator("#test-view .test-content-detail .test-contents>.mt-4>.accordion>.card>.card-header").first()).hasAttribute("aria-expanded","true");
                assertEquals(List.of("test-view"),page.evaluate("()=>window.__coldActivations"));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    @Test void failedNormalizationCannotPublishReadyAndARealRecoveryCan() throws Exception {
        try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                page.setContent("<html><body></body></html>");
                page.addScriptTag(new Page.AddScriptTagOptions().setContent(asset("extent-navigation-performance.js")));
                page.addScriptTag(new Page.AddScriptTagOptions().setContent(asset("extent-navigation-controller.js")));
                assertEquals(Boolean.FALSE,page.evaluate("""
                    () => {atfPerformanceBeginReadiness();normalizeTestPackageMetadata=()=>{throw Error('controlled normalization fault');};
                      try{normalizeTestViewContent();}catch(expected){}return document.documentElement.getAttribute('data-atf-test-view-ready')==='true';}
                    """));
                assertEquals(Boolean.TRUE,page.evaluate("""
                    () => {for(const name of ['normalizeTestPackageMetadata','normalizeScenarioAnnotations','bindPhaseCollapse','bindNodeCollapse',
                      'collapseDirectScenarioCards','collapseScenarioCards','normalizeValidationTableColumns'])window[name]=()=>{};
                      normalizeTestViewContent();return document.documentElement.getAttribute('data-atf-test-view-ready')==='true';}
                    """));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static void assertNavigationTargets(Page page){
        for(var target:Map.of("test","test-view","category","category-view","fails","atf-fails-view","overview","dashboard-view").entrySet())
            assertThat(page.locator("#nav-"+target.getKey())).hasAttribute("href","#"+target.getValue());
    }
    private static void prepare(Page page){
        page.setDefaultTimeout(10000);page.setDefaultNavigationTimeout(45000);
        page.route("http://**/*",Route::abort);page.route("https://**/*",Route::abort);
        page.addInitScript("""
            window.__coldActivations=[];document.addEventListener('DOMContentLoaded',()=>{const activate=window.atfActivate;
              window.atfActivate=function(id){window.__coldActivations.push(id);return activate.apply(this,arguments);};});
            """);
    }
    private static void settle(Page page){page.evaluate("()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))");}
    private static String asset(String name)throws Exception {
        try(var in=ColdStartReadinessComponentTest.class.getResourceAsStream("/atf/reporting/javascript/"+name)){
            return new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);
        }
    }
}
