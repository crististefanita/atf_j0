package atf.ui.tests.extent.support.locators.verdict;

import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Shared DOM boundary for verdict consistency in Extent and the standalone fallback. */
public final class VerdictReportLocators {
    private final ReportUiLocators common = new ReportUiLocators();
    public Locator tests(Page page, boolean fallback) {
        return fallback ? page.locator("button[data-tab='tests']") : common.navTab(page, ReportUiContract.Tab.TESTS);
    }
    public Locator overview(Page page, boolean fallback) {
        return fallback ? page.locator("button[data-tab='overview']") : common.navTab(page, ReportUiContract.Tab.OVERVIEW);
    }
    public Locator category(Page page, boolean fallback) {
        return fallback ? page.locator("button[data-tab='categories']") : common.navTab(page, ReportUiContract.Tab.CATEGORIES);
    }
    public Locator fails(Page page, boolean fallback) {
        return fallback ? page.locator("button[data-tab='fails']") : common.navTab(page, ReportUiContract.Tab.FAILS);
    }
    public Locator metric(Page page, boolean fallback, String label) {
        Locator cards = page.locator(fallback ? "#overview [class~='metric']" : "#dashboard-view [class~='card-body']");
        Locator card = cards.filter(new Locator.FilterOptions().setHas(page.getByText(label,
                new Page.GetByTextOptions().setExact(true))));
        return fallback ? card.locator("strong") : card.getByRole(AriaRole.HEADING);
    }
    public Locator expandAll(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes"));
    }
    public Locator comparisonTable(Page page, boolean fallback) {
        Locator scope = fallback ? page.locator("#tests") : common.visibleTestDetail(page);
        return scope.locator(fallback ? "table[class~='assertions']:visible"
                        : "table[class~='helm-validation-table']:visible:not(.atf-ui-failure-evidence table)")
                .filter(new Locator.FilterOptions().setHasText("Independent comparison"));
    }
    public Locator rows(Locator table) { return table.locator(":scope > tbody > tr"); }
    public Locator valueCells(Locator table) { return table.locator(":scope > tbody > tr > td:nth-child(2), :scope > tbody > tr > td:nth-child(3)"); }
    public Locator failures(Page page, boolean fallback) {
        return fallback ? page.locator("#fails a[class~='fail-row']") : common.failureItems(common.failsView(page));
    }
    public Locator openFailure(Page page, boolean fallback) {
        return fallback ? failures(page, true).first() : common.failureDetailOpenLink(common.failsView(page));
    }
    public Locator scenario(Page page, boolean fallback, String name) {
        return (fallback ? page.locator("#tests article[class~='scenario']") : common.scenarios(common.visibleTestDetail(page)))
                .filter(new Locator.FilterOptions().setHasText(name));
    }
    public Locator scenarioHeader(Locator scenario) { return common.nodeHeader(scenario); }
}
