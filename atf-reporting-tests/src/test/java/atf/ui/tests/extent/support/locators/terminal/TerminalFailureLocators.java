package atf.ui.tests.extent.support.locators.terminal;

import com.microsoft.playwright.*;

/** Typed terminal targets and their published navigation controls. */
public final class TerminalFailureLocators {
    public Locator fails(Page page,boolean fallback) { return page.locator(fallback?"button[data-tab='fails']":"#nav-fails"); }
    public Locator failures(Page page,boolean fallback) { return page.locator(fallback?"#fails [class~='fail-row']":"#atf-failure-list > [class~='test-item']"); }
    public Locator executionFailure(Page page,boolean fallback) {
        return fallback?failures(page,true).filter(new Locator.FilterOptions().setHasText("Execution failure · GET /terminal"))
                :page.locator("#atf-failure-list > [data-atf-failure-kind='execution']");
    }
    public Locator backlink(Page page) { return page.locator("#atf-failure-detail [class~='atf-failure-open']"); }
    public Locator detail(Page page) { return page.locator("#atf-failure-detail"); }
    public Locator terminal(Page page,boolean fallback) {
        return page.locator((fallback?"#tests ":"#test-view [class~='test-content-detail'] ")
                +"[data-atf-terminal-failure='true'][data-atf-request-path='/terminal']");
    }
}
