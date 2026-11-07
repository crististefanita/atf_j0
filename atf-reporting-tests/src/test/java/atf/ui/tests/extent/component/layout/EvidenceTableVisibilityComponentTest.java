package atf.ui.tests.extent.component.layout;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.asset.ReportAssetComposer;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Hidden evidence does not force glyph measurement; opening and reopening still allocate every column. */
@AtfComponentTest
@ReportBrowserTest
class EvidenceTableVisibilityComponentTest {
    @Test void measurementIsDeferredUntilEvidenceBecomesVisible() {
        var ui=new PublishedEvidenceTableLocators();
        try(var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), 
                new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                page.setContent("<html><head><style>"+ReportAssetComposer.tableViewportCss()+"</style></head><body>"
                        +"<details><summary>Evidence</summary><table class='helm-evidence-table'><thead><tr><th>FIELD</th><th>VALUE</th></tr></thead>"
                        +"<tbody><tr><td>Independent observation</td><td>A longer value remains readable</td></tr></tbody></table></details>"
                        +"<script>"+ReportAssetComposer.extentEvidenceTableLayoutScript()+"</script></body></html>");
                page.waitForFunction("() => window.__atfValidationTableLayoutBound === true");
                var disclosure=ui.details(page).first();
                var summary=ui.summary(disclosure);
                var table=ui.dataTables(disclosure);
                assertEquals("DETAILS",table.evaluate("e => e.parentElement.tagName"),"Hidden evidence is not rewritten just to prepare its later layout");
                assertNull(table.getAttribute("data-atf-content-layout"),"Collapsed tables must not trigger intrinsic measurement");
                assertThat(table).not().isVisible();
                summary.click();
                assertThat(table).hasAttribute("data-atf-content-layout","dynamic-longest-value");
                assertThat(table).isVisible();
                assertFalse(table.getAttribute("data-atf-content-column-widths").isBlank());
                summary.click();
                assertThat(table).not().isVisible();
                summary.click();
                assertThat(table).isVisible();
                assertThat(table).hasAttribute("data-atf-content-layout","dynamic-longest-value");
                String measured=table.getAttribute("data-atf-content-column-max-px");
                table.locator("tbody td").nth(1).evaluate("e => e.firstChild.data='A changed original text node with a much longer complete value that must be measured again'");
                page.waitForCondition(() -> !measured.equals(table.getAttribute("data-atf-content-column-max-px")));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    @Test void repeatedPresentationWritesDoNotRemeasureUnchangedCells(){
        try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(10000);
                page.setContent("<html><head><style>"+ReportAssetComposer.tableViewportCss()+".wide{--atf-font-table-cell-size:32px}</style></head><body>"
                    +"<details open><summary>Evidence</summary><table class='helm-evidence-table'><thead><tr><th>FIELD</th><th>VALUE</th></tr></thead>"
                    +"<tbody><tr><td>Expected context</td><td class='stable'>Independent actual value with complete meaning</td></tr></tbody></table></details>"
                    +"<script>window.measureCalls=0;const measure=CanvasRenderingContext2D.prototype.measureText;"
                    +"CanvasRenderingContext2D.prototype.measureText=function(){measureCalls++;return measure.apply(this,arguments);};</script>"
                    +"<script>"+ReportAssetComposer.extentEvidenceTableLayoutScript()+"</script></body></html>");
                var table=page.locator("table");assertThat(table).hasAttribute("data-atf-content-layout","dynamic-longest-value");settle(page);
                Object before=page.evaluate("()=>window.measureCalls");
                page.evaluate("""
                    () => {const cell=document.querySelector('td.stable'),detail=document.querySelector('details');
                      for(let i=0;i<200;i++){cell.classList.add('stable');detail.setAttribute('open','');}}
                    """);settle(page);
                assertEquals(before,page.evaluate("()=>window.measureCalls"),"Repeated identical presentation writes cannot trigger glyph measurement");
                String measured=table.getAttribute("data-atf-content-column-max-px");
                page.locator("td.stable").evaluate("e=>e.classList.add('wide')");
                assertEquals("32px",page.locator("td.stable").evaluate("e=>getComputedStyle(e).fontSize"));
                page.waitForCondition(()->!measured.equals(table.getAttribute("data-atf-content-column-max-px")));
                assertThat(table.locator("tbody td").last()).hasText("Independent actual value with complete meaning");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    @Test void hiddenViewsAvoidGeometryReadsAndMeasureWhenRevealed(){
        try(var pwLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(pwLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))){
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(10000);
                page.setContent("<html><head><style>"+ReportAssetComposer.tableViewportCss()+".d-none{display:none}</style></head><body>"
                    +"<section class='d-none'><table class='helm-evidence-table'><thead><tr><th>FIELD</th><th>VALUE</th></tr></thead>"
                    +"<tbody><tr><td>Hidden source</td><td>Complete original actual</td></tr></tbody></table></section>"
                    +"<script>window.tableGeometryReads=0;const rects=Element.prototype.getClientRects;"
                    +"Element.prototype.getClientRects=function(){if(this.tagName==='TABLE')tableGeometryReads++;return rects.apply(this,arguments);};</script>"
                    +"<script>"+ReportAssetComposer.extentEvidenceTableLayoutScript()+"</script></body></html>");
                page.waitForFunction("()=>window.__atfValidationTableLayoutBound===true");settle(page);
                assertEquals(0,((Number)page.evaluate("()=>window.tableGeometryReads")).intValue(),"Known hidden source views must not enter geometry measurement");
                var table=page.locator("table");assertNull(table.getAttribute("data-atf-content-layout"));
                page.locator("section").evaluate("e=>e.classList.remove('d-none')");
                assertThat(table).hasAttribute("data-atf-content-layout","dynamic-longest-value");assertThat(table).isVisible();
                assertThat(table.locator("tbody td").last()).hasText("Complete original actual");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static void settle(Page page){page.evaluate("()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))");}
}
