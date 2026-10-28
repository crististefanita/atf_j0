package atf.ui.tests.extent.support.locators.failurecontext;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** DOM vocabulary for failure context; test logic stays independent of selector strings. */
public final class ReportFailureContextLocators {
    public Locator assertionRows(Page page) { return page.locator("table:is([class~='assertions'],[class~='helm-validation-table']):visible tbody tr"); }
    public Locator actionRows(Page page) { return page.locator("table[class~='atf-ui-action-table']:visible tbody tr"); }
    public Locator observationRows(Page page) { return page.locator("table[class~='atf-ui-observation-table']:visible tbody tr"); }
    public Locator cells(Locator row) { return row.locator("td"); }
    public Locator observations(Page page) { return page.locator("details[class~='atf-ui-observations']:visible"); }
    public Locator summary(Locator details) { return details.locator(":scope > summary"); }
    public Locator fallbackCause(Page page) { return page.locator("#fails [class~='fail-row'] > span:last-child"); }
}
