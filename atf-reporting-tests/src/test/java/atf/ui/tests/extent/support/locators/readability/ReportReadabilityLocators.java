package atf.ui.tests.extent.support.locators.readability;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

/** Selector boundary for evidence readability; measurements and assertions remain in the probe. */
public final class ReportReadabilityLocators {
    public Locator expandAll(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes"));
    }
    public Locator details(Page page) { return page.locator("details"); }
    public Locator failureEvidence(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Failure evidence")));
    }
    public Locator panel(Page page, boolean fallback) {
        return page.locator(fallback ? "[id='tests']" : "[class~='test-content-detail']:visible");
    }
    public Locator failsTab(Page page) { return page.locator("[id='nav-fails']"); }
    public Locator comparisonFailure(Page page, int ordinal) {
        return page.locator("#atf-failure-list > [data-atf-failure-kind='assertion'][data-atf-failure-ordinal='" + ordinal + "']");
    }
    public Locator testsView(Page page) { return page.locator("[id='test-view']"); }
    public Locator firstVisibleSummary(Locator request) { return request.locator("details:visible > summary").first(); }
}
