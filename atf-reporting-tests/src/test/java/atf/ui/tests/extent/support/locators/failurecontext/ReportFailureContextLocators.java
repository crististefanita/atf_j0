package atf.ui.tests.extent.support.locators.failurecontext;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** DOM vocabulary for failure context; test logic stays independent of selector strings. */
public final class ReportFailureContextLocators {
    public Locator assertionRows(Page page) { return page.locator("table:is([class~='assertions'],[class~='helm-validation-table']):visible tbody tr"); }
    public Locator actionRows(Page page) { return page.locator("table[class~='atf-ui-action-table']:visible tbody tr"); }
    public Locator observationRows(Page page) { return page.locator("table[class~='atf-ui-observation-table']:visible tbody tr"); }
    public Locator cells(Locator row) { return row.locator("td"); }
    public Locator operation(Locator row) { return row.locator("[class~='atf-ui-action-operation']"); }
    public Locator sequence(Locator row) { return row.locator("[class~='atf-ui-action-number']"); }
    public Locator result(Locator row) { return row.locator("[class~='atf-ui-action-result']"); }
    public Locator observations(Page page) { return page.locator("details[class~='atf-ui-observations']:visible"); }
    public Locator summary(Locator details) { return details.locator(":scope > summary"); }
    public Locator fallbackCause(Page page) { return page.locator("#fails [class~='fail-row'] > span:last-child"); }
    public Locator diagnostic(Locator scope, String relation) {
        return scope.locator("table[class~='atf-ui-observation-table']:visible tbody tr")
                .filter(new Locator.FilterOptions().setHasText(relation));
    }
    public Locator allObservations(Locator scope) { return scope.locator("details[class~='atf-ui-observations']"); }
    public Locator actionDetails(Locator scope) { return scope.locator("details[class~='atf-ui-action-sequence']"); }
    public Locator assertionDetails(Locator scope) { return scope.locator("details[class~='assertion-evidence']"); }
    public Locator httpDetails(Locator scope) {
        return scope.locator("details:is([class~='request-evidence'],[class~='response-evidence'])");
    }
}
