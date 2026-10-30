package atf.ui.tests.extent.support.locators.failurenavigation;

import com.microsoft.playwright.*;
import java.util.Locale;

/** Exact navigation surfaces; the oracle never invokes production disclosure helpers. */
public final class FailureNavigationLocators {
    public Locator fails(Page page) { return page.locator("#nav-fails"); }
    public Locator failures(Page page) { return page.locator("#atf-failure-list > .test-item"); }
    public Locator backlink(Page page) { return page.locator("#atf-failure-detail .atf-failure-open"); }
    public Locator phase(Page page, String name) {
        return scenarios(page).nth(1).locator("[data-atf-node-role='phase'][data-atf-phase-name='"
                + name.toLowerCase(Locale.ROOT).replace(' ', '-') + "']");
    }
    public Locator target(Page page, String phase) { return phase(page, phase).locator(".helm-validation-table tbody tr").nth(3); }
    public Locator sibling(Page page) { return scenarios(page).first(); }
    public Locator fallbackFails(Page page) { return page.locator("button[data-tab='fails']"); }
    public Locator fallbackFailures(Page page) { return page.locator("#fails .fail-row"); }
    public Locator fallbackTarget(Page page, String name) {
        return fallbackScenario(page).locator("[data-atf-node-role='phase'][data-atf-phase-name='"
                + name.toLowerCase(Locale.ROOT).replace(' ', '-') + "'] table.assertions tbody tr").nth(3);
    }
    public Locator diagnostic(Page page, boolean fallback, String phase) {
        return (fallback ? fallbackScenario(page) : scenarios(page).nth(1))
                .locator(".phase-diagnostics [data-atf-phase-name='" + phase + "'] tbody tr");
    }
    public Locator assertionRows(Page page, boolean fallback) {
        return (fallback ? fallbackScenario(page) : scenarios(page).nth(1))
                .locator(fallback ? "table.assertions tbody tr" : ".helm-validation-table tbody tr");
    }
    public Locator cause(Page page) { return page.locator("#atf-failure-detail .atf-failure-index-cause .atf-failure-value"); }
    private Locator fallbackScenario(Page page) { return page.locator("#tests article.scenario").nth(1); }
    private Locator scenarios(Page page) { return page.locator("#test-view .test-content-detail .test-contents > .mt-4 > .accordion > .card"); }
}
