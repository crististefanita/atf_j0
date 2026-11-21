package atf.shop.tests.support.reporting;

import atf.core.ui.AtfUiActionRecord;
import atf.shop.locators.reporting.ShopRecoveryReportLocators;
import atf.testkit.assertions.RecordedAssertions;
import com.microsoft.playwright.Page;

import java.nio.file.Path;
import java.util.List;

/** Owns selectors for the local Extent report rendered by recovery component tests. */
public final class ShopRecoveryExtentReportProbe {
    private static final ShopRecoveryReportLocators LOCATORS = new ShopRecoveryReportLocators();

    private ShopRecoveryExtentReportProbe() { }

    public static void verify(Page page, Path reportPath, String stepTitle,
                              List<AtfUiActionRecord> records, RecordedAssertions checks) {
        page.navigate(reportPath.toUri().toString());
        LOCATORS.testsTab(page).click();
        var reportTest = LOCATORS.reportTest(page, "ShopRecoveryReport");
        checks.isTrue("The generated recovery Extent test is listed", reportTest.isVisible());
        LOCATORS.owningTestItem(reportTest).click();

        var detail = LOCATORS.detailBody(page);
        var group = LOCATORS.observationGroup(detail);
        checks.equals("Recovery attempts render in one collapsed observation group", 1, group.count());
        checks.isFalse("The recovery group starts collapsed", LOCATORS.isOpen(group));
        LOCATORS.expandAllNodes(page).click();
        checks.isFalse("Expand all nodes preserves the observation disclosure state", LOCATORS.isOpen(group));

        var step = LOCATORS.outcomeStep(detail, stepTitle);
        checks.isTrue("The recovery outcome is visible at the parent step", step.isVisible());
        var summary = LOCATORS.summary(group);
        checks.isTrue("The collapsed summary identifies the attempt count",
                summary.innerText().contains(Integer.toString(records.size())));
        summary.click();

        var rows = LOCATORS.attemptRows(group);
        checks.equals("Expanding reveals every recovery attempt", records.size(), rows.count());
        for (int index = 0; index < records.size(); index++) {
            var cells = LOCATORS.cells(rows.nth(index));
            checks.equals("Attempt order and name are preserved: " + (index + 1),
                    records.get(index).operation(), LOCATORS.operation(cells).innerText());
            checks.equals("Policy source and budget appear once: " + (index + 1),
                    records.get(index).expected(), LOCATORS.policy(cells).innerText());
            checks.equals("Complete decision, signal, reason, and timing appear once: " + (index + 1),
                    records.get(index).observed(), LOCATORS.observation(cells).innerText());
            checks.equals("Recovery observation remains informational: " + (index + 1), "Info",
                    LOCATORS.result(cells).getAttribute("aria-label"));
        }
        summary.click();
        checks.isFalse("Recovery observations collapse after inspection", LOCATORS.isOpen(group));
        checks.isFalse("Collapsing hides every recovery attempt", rows.first().isVisible());
    }
}
