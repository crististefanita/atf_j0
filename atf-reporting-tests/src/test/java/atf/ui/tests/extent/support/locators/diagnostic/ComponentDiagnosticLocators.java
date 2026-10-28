package atf.ui.tests.extent.support.locators.diagnostic;

import com.microsoft.playwright.*;

/** Selectors for an execution diagnostic without an invented failing comparison. */
public final class ComponentDiagnosticLocators {
    public Locator nativeFailures(Page page) { return page.locator("[id='atf-failure-list'] [data-atf-failure-id]"); }
    public Locator failureIndex(Page page) { return page.locator("[id='atf-failure-detail'] [class~='atf-failure-index']"); }
    public Locator labels(Page page) { return failureIndex(page).locator("[class~='atf-failure-label']"); }
    public Locator fallbackFailures(Page page) { return page.locator("[id='fails'] a[class~='fail-row']"); }
    public Locator assertionValues(Locator table) {
        return table.locator(":scope > tbody > tr > td:nth-child(2), :scope > tbody > tr > td:nth-child(3)");
    }
}
