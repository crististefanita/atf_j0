package atf.ui.tests.extent.support.locators.execution;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** DOM vocabulary for typed terminal API evidence, shared by the two renderer probes. */
public final class TerminalExecutionLocators {
    public Locator testsTab(Page page, boolean fallback) {
        return page.locator(fallback ? "button[data-tab='tests']" : "#nav-test");
    }
    public Locator expandAll(Page page) { return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes")); }
    public Locator execution(Page page) { return page.locator("details[class~='execution-evidence']:visible"); }
    public Locator summary(Locator disclosure) { return disclosure.locator(":scope > summary"); }
    public Locator attempts(Locator disclosure) { return disclosure.locator("table[class~='retry-history-table'] tbody tr"); }
    public Locator cells(Locator row) { return row.locator(":scope > td"); }
    public Locator executionSummary(Locator disclosure) { return disclosure.locator("table[class~='execution-table']"); }
    public Locator responseDisclosures(Page page) { return page.locator("details[class~='response-evidence']"); }
}
