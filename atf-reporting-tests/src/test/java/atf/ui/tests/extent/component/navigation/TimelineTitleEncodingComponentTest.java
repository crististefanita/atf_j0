package atf.ui.tests.extent.component.navigation;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.TestReportPaths;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jsoup.Jsoup;
import java.nio.file.*;
import java.util.*;
import static atf.reporting.contract.AtfReportStatus.PASS;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Native Spark timeline keys retain full caller labels without generating invalid JavaScript. */
@AtfComponentTest
@ReportBrowserTest
class TimelineTitleEncodingComponentTest {
    @ParameterizedTest
    @ValueSource(strings={"sameName [{\n\"caseId\" : \"segmented\"\n}]","Quoted \"owner\" at C:\\reports\\case","Ședință 🚀\r\nsecond line\u2028end"})
    void nativeTimelinePreservesQuotedMultilineAndUnicodeNames(String name)throws Exception{
        Path file=TestReportPaths.report("timeline-title-"+UUID.randomUUID()+".html");
        try(var report=new ExtentReportSession(file)){
            var scenario=report.startScenario(name,"Independent title contract",List.of("component"));scenario.complete(PASS);
        }
        String script=Jsoup.parse(Files.readString(file)).select("script").stream().map(e->e.data())
            .filter(text->text.contains("var timeline = {")).findFirst().orElseThrow();
        try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                var errors=new ArrayList<String>();page.onPageError(errors::add);page.setDefaultTimeout(10000);page.setDefaultNavigationTimeout(45000);
                page.route("http://**/*",Route::abort);page.route("https://**/*",Route::abort);page.navigate(file.toUri()+"#test-view");
                page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");
                Object keys=page.evaluate("source=>Object.keys(Function(source+';return timeline;')())",script);
                assertEquals(List.of(name),keys,"Native JavaScript must preserve the exact full owner label without clipping or whitespace replacement");
                page.locator("#nav-overview").click();assertThat(page.locator("#dashboard-view")).isVisible();
                assertTrue(errors.isEmpty(),errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}
