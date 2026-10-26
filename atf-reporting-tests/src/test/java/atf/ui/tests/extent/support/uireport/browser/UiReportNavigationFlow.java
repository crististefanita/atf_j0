package atf.ui.tests.extent.support.uireport.browser;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Objects;

/** Semantic navigation for UI-report evidence; no selector knowledge leaks to tests. */
public final class UiReportNavigationFlow {
    private final UiAuditFlow audit;
    private final ReportUiLocators locators;

    public UiReportNavigationFlow(UiAuditFlow audit, ReportUiLocators locators) {
        this.audit = Objects.requireNonNull(audit, "audit");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Locator openFirstOwner(Page page) {
        if (locators.testOwners(page).count() == 0) return locators.visibleTestDetail(page);
        audit.actions().click(locators.firstTestOwnerAction(page),
                "first UI report owner opens");
        Locator detail = locators.visibleTestDetail(page);
        audit.actions().waitForVisible(detail, "#test-view .test-content-detail:visible");
        return detail;
    }

    /** Expands only structural ancestors needed to inspect UI validation. */
    public void expandTestStructure(Locator detail) {
        for (int scenarioIndex = 0; scenarioIndex < locators.scenarios(detail).count(); scenarioIndex++) {
            Locator scenario = locators.scenarios(detail).nth(scenarioIndex);
            expand(scenario, locators.scenarioNodeAction(scenario),
                    "UI report scenario evidence opens");
            Locator phases = locators.phases(scenario);
            for (int phaseIndex = 0; phaseIndex < phases.count(); phaseIndex++) {
                Locator phase = phases.nth(phaseIndex);
                expand(phase, locators.phaseNodeAction(phase),
                        "UI report phase evidence opens");
            }
        }
    }

    private void expand(Locator card, AtfLocator action, String description) {
        if (card.count() == 0 || !card.isVisible()) return;
        String state = card.getAttribute(locators.nodeOpenAttribute(card));
        if (!"true".equals(state)) audit.actions().click(action, description);
    }
}
