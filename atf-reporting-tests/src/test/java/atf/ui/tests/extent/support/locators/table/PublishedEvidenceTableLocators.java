package atf.ui.tests.extent.support.locators.table;

import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.FallbackTab;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.locators.readability.ReportReadabilityLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Complete visible inventory, including data tables that have lost their layout markers. */
public final class PublishedEvidenceTableLocators {
    private final ReportUiLocators common = new ReportUiLocators();
    private final ReportReadabilityLocators readability = new ReportReadabilityLocators();

    public Locator tables(Page page) { return page.locator("table:visible"); }
    public Locator dataTables(Locator scope) { return scope.locator("table[class~='helm-evidence-table']"); }
    public Locator firstOwner(Page page) { return common.testOwners(page).first(); }
    public Locator expandAll(Page page) { return readability.expandAll(page); }
    public Locator details(Page page) { return page.locator("details:visible"); }
    public Locator closedDetails(Page page) { return page.locator("details:not([open]):visible"); }
    public Locator summaries(Page page) { return details(page).locator(":scope > summary"); }
    public Locator assertions(Page page) {
        return page.locator("table[class~='helm-validation-table']:visible,table[class~='assertions']:visible");
    }
    public Locator rows(Locator table) { return table.locator(":scope > tbody > tr"); }
    public Locator viewport(Locator table) { return table.locator(".."); }
    public Locator summary(Locator detail) { return detail.locator(":scope > summary"); }
    public Locator panel(Page page, boolean fallback) { return readability.panel(page, fallback); }
    public Locator tab(Page page, boolean fallback, ReportUiContract.Tab tab) {
        return fallback ? page.locator("button[data-tab='" + fallbackTab(tab).value() + "']") : common.navTab(page, tab);
    }
    public Locator firstCategory(Page page) { return common.categoryItems(page).first(); }
    public Locator firstFailure(Page page) { return common.failureItems(common.failsView(page)).first(); }
    public Locator failureDetail(Page page) { return common.failureDetail(common.failsView(page)); }
    public Locator failurePanels(Page page) { return common.failsView(page).locator(":scope > [class~='test-list'],:scope > [class~='test-content']"); }
    public Locator view(Page page, boolean fallback, ReportUiContract.Tab tab) {
        return fallback ? page.locator("[id='" + fallbackTab(tab).value() + "']") : common.tabView(page, tab);
    }
    private static FallbackTab fallbackTab(ReportUiContract.Tab tab) {
        return tab == ReportUiContract.Tab.CATEGORIES ? FallbackTab.CATEGORY : FallbackTab.valueOf(tab.name());
    }
}

