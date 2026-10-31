package atf.ui.tests.extent.support.locators.disclosure;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Active owner only: hidden original Extent nodes and sibling comparisons cannot satisfy this oracle. */
public final class ApiValidationDisclosureLocators {
    private final ReportUiLocators common = new ReportUiLocators();
    public Locator scenario(Page page, boolean fallback, String title) {
        var scenarios = fallback ? page.locator("article[class~='scenario']") : common.scenarios(common.visibleTestDetail(page));
        return scenarios.filter(new Locator.FilterOptions().setHasText(title)).first();
    }
    public void selectTests(Page page, boolean fallback) {
        if (fallback) page.locator("button[data-tab='tests']").click();
        else {
            common.testOwners(page).first().click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
        }
    }
    public void openAncestors(Locator scenario, boolean fallback) {
        if (fallback) return;
        for (String role : new String[]{"scenario", "phase", "request"}) {
            Locator card = role.equals("scenario") ? scenario : scenario.locator("[data-atf-node-role='" + role + "']").first();
            String state = role.equals("scenario") ? "data-atf-scenario-open" : "data-atf-node-open";
            if (role.equals("phase")) state = "data-atf-phase-open";
            if (!"true".equals(card.getAttribute(state))) common.requestNodeHeader(card).click();
        }
    }
    public Locator validation(Locator scenario) { return scenario.locator("[data-atf-node-role='validation']").first(); }
    public Locator table(Locator validation) {
        return validation.locator("table[class~='assertions'],table[class~='helm-validation-table']:not(details table)");
    }
    public Locator disclosure(Locator table, boolean fallback) {
        return table.locator(fallback ? "xpath=ancestor::details[1]"
                : "xpath=ancestor::*[@data-atf-node-role='validation'][1]");
    }
    public Locator summary(Locator disclosure, boolean fallback) {
        return fallback ? disclosure.locator(":scope > summary") : common.requestNodeHeader(disclosure);
    }
    public boolean isOpen(Locator disclosure, boolean fallback) {
        return fallback ? (Boolean) disclosure.evaluate("d => d.open")
                : "true".equals(disclosure.getAttribute("data-atf-node-open"));
    }
    public Locator rows(Locator table) { return table.locator("tbody > tr"); }
    public Locator cells(Locator row) { return row.locator("td"); }
    public Locator headings(Locator table) { return table.locator("thead th"); }
    public void swapFirstTwoResults(Locator table) {
        table.evaluate("t => {const a=t.tBodies[0].rows[0].cells[3],b=t.tBodies[0].rows[1].cells[3];"
                + "const saved=a.innerHTML;a.innerHTML=b.innerHTML;b.innerHTML=saved;}");
    }
    public void followFailure(Page page, boolean fallback) {
        page.locator(fallback ? "button[data-tab='fails']" : "[id='nav-fails']").click();
        if (fallback) page.locator("[id='fails'] [class~='fail-row']").last().click();
        else {
            page.locator("[id='atf-failure-list'] > [class~='test-item']").last().click();
            page.locator("[id='atf-failure-detail'] [class~='atf-failure-open']").click();
        }
    }
    public void mutate(Locator table, boolean fallback, String mutation) {
        switch (mutation) {
            case "missing-disclosure" -> disclosure(table, fallback).evaluate("d => d.replaceWith(...d.childNodes)");
            case "initially-open" -> summary(disclosure(table, fallback), fallback).click();
            case "stolen-final-row" -> table.evaluate("t => {const s=t.closest('[data-atf-scenario-id],article.scenario');"
                    + "const other=[...s.parentElement.querySelectorAll('table.assertions,table.helm-validation-table')]"
                    + ".find(x=>x!==t&&!s.contains(x));"
                    + "if(!other)throw Error('No sibling table');other.tBodies[0].append(t.tBodies[0].lastElementChild);}");
            default -> throw new IllegalArgumentException(mutation);
        }
    }
}
