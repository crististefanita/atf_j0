package atf.ui.tests.extent.support.locators.query;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Public assertion-step disclosure in the controlled query fixture. */
public final class QueryReportLocators {
    public Locator fallbackAssertions(Page page) {
        return page.locator("#tests details[data-atf-node-role='assertion-step']")
                .filter(new Locator.FilterOptions().setHasText("Independent comparison"));
    }
    public Locator summary(Locator disclosure) { return disclosure.locator(":scope > summary"); }
}
