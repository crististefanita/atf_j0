package atf.ui.tests.extent.support.locators.table;

import atf.reporting.markup.table.ReportTableViewport;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Single selector boundary for the data-table inventory, never the vendor event/layout tables. */
public final class TableViewportLocators {
    private final ReportUiLocators common = new ReportUiLocators();
    public static final String VIEWPORTS = ReportTableViewport.selectors();

    public Locator firstOwner(Page page) { return common.testOwners(page).first(); }
    public Locator selected(Page page) { return common.visibleTestDetail(page); }
    public Locator componentTables(Page page) { return page.locator("[data-family] table"); }
    public Locator system(Page page) {
        return page.locator("[class~='dashboard-view'] [class~='sysenv-container'] > [class~='card'] > [class~='card-body']");
    }
    public Locator dataTables(Locator scope) {
        return scope.locator("table[class~='helm-evidence-table'],table[class~='atf-setup-evidence-table'],"
                + "table[class~='atf-ui-action-table'],table[class~='assertions']");
    }
    public Locator nativeCards(Locator scope) {
        return scope.locator("[data-atf-node-role],[data-atf-scenario-id]");
    }
    public Locator headers(Locator card) { return common.requestNodeHeader(card); }
    public Locator disclosures(Locator scope) { return scope.locator("details"); }
    public Locator screenshots(Locator scope) { return scope.locator("[class~='atf-ui-action-screenshot']"); }
    public Locator fallbackTests(Page page) { return page.locator("button[data-tab='tests']"); }
    public Locator fallbackContent(Page page) { return page.locator("[id='tests']"); }
    public Locator summaries(Locator scope) { return scope.locator("summary"); }
    public Locator actions(Locator scope) { return scope.locator("table[class~='atf-ui-action-table']"); }
    public Locator setup(Locator scope) { return scope.locator("table[class~='atf-setup-evidence-table']"); }
    public Locator viewport(Locator table) { return table.locator(".."); }
    public Locator assertions(Locator scope) {
        return scope.locator("table[class~='helm-validation-table'],table[class~='assertions']");
    }
}
