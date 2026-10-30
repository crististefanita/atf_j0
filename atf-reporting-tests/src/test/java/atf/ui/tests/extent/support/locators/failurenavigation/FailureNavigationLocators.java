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
    private Locator scenarios(Page page) { return page.locator("#test-view .test-content-detail .test-contents > .mt-4 > .accordion > .card"); }
}
