package atf.ui.tests.extent.support.locators.flow;

import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Published DOM surfaces only; no production navigation or grouping helper is invoked. */
public final class BodyOnlyUnifiedFlowLocators {
    private final ReportUiLocators common = new ReportUiLocators();
    private final PublishedEvidenceTableLocators tables = new PublishedEvidenceTableLocators();
    public Locator tests(Page page, boolean fallback) { return tables.tab(page, fallback, ReportUiContract.Tab.TESTS); }
    public Locator owner(Page page) { return common.testOwners(page); }
    public Locator expandAll(Page page) { return tables.expandAll(page); }
    public Locator root(Page page, boolean fallback) {
        return page.locator(fallback ? "[id='tests'] article[class~='scenario']"
                : "[id='test-view'] [class~='test-content-detail']:visible");
    }
    public Locator flows(Page page, boolean fallback) { return root(page, fallback).locator("[data-atf-node-role='request']"); }
    public Locator validations(Page page, boolean fallback) { return root(page, fallback).locator("[data-atf-node-role='validation']"); }
    public Locator nestedValidations(Locator flow) { return flow.locator("[data-atf-node-role='validation']"); }
    public Locator closedDetails(Page page, boolean fallback) { return root(page, fallback).locator("details:not([open]):visible"); }
    public Locator summary(Locator detail) { return detail.locator(":scope > summary"); }
    public Locator actions(Page page, boolean fallback) {
        return root(page, fallback).locator("table[class~='atf-ui-action-table']:not([class~='atf-ui-observation-table'])");
    }
    public Locator observations(Page page, boolean fallback) { return root(page, fallback).locator("table[class~='atf-ui-observation-table']"); }
    public Locator assertions(Page page, boolean fallback) {
        return root(page, fallback).locator(fallback ? "table[class~='assertions']" : "table[class~='helm-validation-table']");
    }
    public Locator rows(Locator table) { return table.locator(":scope > tbody > tr"); }
    public Locator cells(Locator row) { return row.locator(":scope > td"); }
    public Locator images(Page page, boolean fallback) { return root(page, fallback).locator("[data-atf-screenshot='base64']"); }
    public Locator compactTrace(Page page, boolean fallback) { return root(page, fallback).locator("[class~='atf-ui-flow-provenance']"); }
    public Locator assertionDisclosure(Locator table) { return table.locator("xpath=ancestor::details[1]"); }
}
