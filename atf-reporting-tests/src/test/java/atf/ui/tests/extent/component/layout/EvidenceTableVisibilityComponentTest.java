package atf.ui.tests.extent.component.layout;

import atf.reporting.asset.ReportAssetComposer;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Hidden evidence does not force glyph measurement; opening and reopening still allocate every column. */
@AtfComponentTest
class EvidenceTableVisibilityComponentTest {
    @Test void measurementIsDeferredUntilEvidenceBecomesVisible() {
        var ui=new PublishedEvidenceTableLocators();
        try(var playwright=Playwright.create();var browser=playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));var page=browser.newPage()){
            page.setContent("<html><head><style>"+ReportAssetComposer.tableViewportCss()+"</style></head><body>"
                    +"<details><summary>Evidence</summary><table class='helm-evidence-table'><thead><tr><th>FIELD</th><th>VALUE</th></tr></thead>"
                    +"<tbody><tr><td>Independent observation</td><td>A longer value remains readable</td></tr></tbody></table></details>"
                    +"<script>"+ReportAssetComposer.extentEvidenceTableLayoutScript()+"</script></body></html>");
            page.waitForFunction("() => window.__atfValidationTableLayoutBound === true");
            var disclosure=ui.details(page).first();
            var summary=ui.summary(disclosure);
            var table=ui.dataTables(disclosure);
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
        }
    }
}
