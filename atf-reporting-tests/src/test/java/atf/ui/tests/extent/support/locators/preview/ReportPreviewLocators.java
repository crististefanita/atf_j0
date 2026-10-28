package atf.ui.tests.extent.support.locators.preview;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** DOM vocabulary for decoded preview values and navigation, shared by budget probes. */
public final class ReportPreviewLocators {
    public Locator fallbackFailsTab(Page page) { return page.locator("button[data-tab='fails']"); }
    public Locator fallbackCauses(Page page) { return page.locator("#fails [class~='fail-row'] > span"); }
    public Locator fallbackFailureLinks(Page page) { return page.locator("#fails [class~='fail-row']"); }
    public Locator failsTab(Page page) { return page.locator("#nav-fails"); }
    public Locator failureItems(Page page) { return page.locator("#atf-failure-list > [class~='test-item']"); }
    public Locator failureCause(Page page) { return page.locator("[class~='atf-failure-index-cause'] [class~='atf-failure-value']"); }
    public Locator openFailureInTests(Page page) { return page.locator("[class~='atf-failure-open']"); }
    public Locator summaries(Page page) { return page.locator("details > summary"); }
    public Locator expandAllNodes(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes"));
    }
}
