package atf.shop.locators.reporting;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Test-only locator facade for the recovery Extent report rendered by Shop fixtures. */
public final class ShopRecoveryReportLocators {
    public Locator testsTab(Page page) { return page.locator("#nav-test"); }

    public Locator reportTest(Page page, String reportName) {
        return page.locator("p.name").filter(new Locator.FilterOptions().setHasText(reportName));
    }

    public Locator owningTestItem(Locator reportTest) {
        return reportTest.locator("xpath=ancestor::li[contains(concat(' ', normalize-space(@class), ' '), ' test-item ')][1]");
    }

    public Locator detailBody(Page page) { return page.locator("#test-view .test-content-detail .detail-body"); }
    public Locator observationGroup(Locator detail) { return detail.locator("details.atf-ui-observations").last(); }
    public Locator expandAllNodes(Page page) {
        return page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Expand all nodes"));
    }
    public Locator outcomeStep(Locator detail, String stepTitle) {
        return detail.locator("a.node").filter(new Locator.FilterOptions().setHasText(stepTitle)).last();
    }
    public Locator summary(Locator group) { return group.locator("summary"); }
    public Locator attemptRows(Locator group) { return group.locator("table.atf-ui-action-table tbody tr"); }
    public Locator cells(Locator row) { return row.locator("td"); }
    public Locator operation(Locator cells) { return cells.nth(0).locator(".atf-ui-action-operation"); }
    public Locator policy(Locator cells) { return cells.nth(2); }
    public Locator observation(Locator cells) { return cells.nth(3); }
    public Locator result(Locator cells) { return cells.nth(4).locator(".atf-ui-action-result"); }
    public boolean isOpen(Locator disclosure) {
        return (Boolean) disclosure.evaluate("element => element.open");
    }
}
