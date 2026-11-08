package atf.ui.tests.extent.support.locators.failurenavigation;

import com.microsoft.playwright.*;
import java.util.Locale;

/** Exact navigation surfaces; the oracle never invokes production disclosure helpers. */
public final class FailureNavigationLocators {
    public Locator fails(Page page) { return page.locator("#nav-fails"); }
    public Locator failures(Page page) { return page.locator("#atf-failure-list > [class~='test-item']"); }
    public Locator backlink(Page page) { return page.locator("#atf-failure-detail [class~='atf-failure-open']"); }
    public Locator phase(Page page, String name) {
        return phase(extentScenario(page), name);
    }
    public Locator phase(Locator scenario, String name) {
        return scenario.locator("[data-atf-node-role='phase'][data-atf-phase-name='"
                + name.toLowerCase(Locale.ROOT).replace(' ', '-') + "']");
    }
    public Locator extentScenario(Page page) { return scenarios(page).nth(1); }
    public Locator diagnosticDisclosures(Locator owner) { return owner.locator("details[class~='phase-diagnostics']"); }
    public Locator diagnosticSummary(Locator disclosure) { return disclosure.locator(":scope > summary"); }
    public Locator diagnostic(Locator disclosure, String phase) {
        return disclosure.locator("[data-atf-phase-name='" + phase + "'] tbody tr");
    }
    public Locator target(Page page, String phase) { return phase(page, phase).locator("[class~='helm-validation-table'] tbody tr").nth(3); }
    public Locator sibling(Page page) { return scenarios(page).first(); }
    public Locator siblingBody(Page page) { return sibling(page).locator(":scope > [class~='accordion']"); }
    public Locator fallbackFails(Page page) { return page.locator("button[data-tab='fails']"); }
    public Locator fallbackFailures(Page page) { return page.locator("#fails [class~='fail-row']"); }
    public Locator fallbackFailurePreview(Locator row) { return row.locator(":scope > span").last(); }
    public Locator fallbackTarget(Page page, String name) {
        return fallbackScenario(page).locator("[data-atf-node-role='phase'][data-atf-phase-name='"
                + name.toLowerCase(Locale.ROOT).replace(' ', '-') + "'] table[class~='assertions'] tbody tr").nth(3);
    }
    public Locator diagnostic(Page page, boolean fallback, String phase) {
        return (fallback ? fallbackScenario(page) : extentScenario(page))
                .locator("[class~='phase-diagnostics'] [data-atf-phase-name='" + phase + "'] tbody tr");
    }
    public Locator assertionRows(Page page, boolean fallback) {
        return (fallback ? fallbackScenario(page) : scenarios(page).nth(1))
                .locator(fallback ? "table[class~='assertions'] tbody tr" : "[class~='helm-validation-table'] tbody tr");
    }
    public Locator cause(Page page) { return page.locator("#atf-failure-detail [class~='atf-failure-index-cause'] [class~='atf-failure-value']"); }
    public Locator cells(Locator row) { return row.locator("td"); }
    public Locator originalRepeatedFailure(Page page, int index) {
        return scenarios(page).nth(1).locator("[data-atf-node-role='validation']").nth(index)
                .locator("table[class~='helm-validation-table']").first().locator("tbody > tr");
    }
    public Locator failedStatus(Locator row) { return cells(row).nth(3).locator("[class~='status-fail']"); }
    private Locator fallbackScenario(Page page) { return page.locator("#tests article[class~='scenario']").nth(1); }
    private Locator scenarios(Page page) { return page.locator("#test-view [class~='test-content-detail'] [class~='test-contents'] > [class~='mt-4'] > [class~='accordion'] > [class~='card']"); }
    private static String phaseName(String name) { return name.toLowerCase(Locale.ROOT).replace(' ', '-'); }
}
