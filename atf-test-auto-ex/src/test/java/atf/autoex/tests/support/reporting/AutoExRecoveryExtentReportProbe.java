package atf.autoex.tests.support.reporting;

import atf.core.ui.AtfUiActionRecord;
import atf.testkit.assertions.RecordedAssertions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Path;
import java.util.List;

/** Owns selectors for the local Extent report rendered by recovery component tests. */
public final class AutoExRecoveryExtentReportProbe {
    private AutoExRecoveryExtentReportProbe() { }

    public static void verify(Page page, Path reportPath, String stepTitle,
                              List<AtfUiActionRecord> records, RecordedAssertions checks) {
        page.navigate(reportPath.toUri().toString());
        page.locator("#nav-test").click();
        var reportTest = page.locator("p.name:text-is('AutoExRecoveryReport')");
        checks.isTrue("The generated recovery Extent test is listed", reportTest.isVisible());
        reportTest.locator("xpath=ancestor::li[contains(concat(' ', normalize-space(@class), ' '), ' test-item ')][1]")
                .click();

        var detail = page.locator("#test-view .test-content-detail .detail-body");
        var group = detail.locator("details.atf-ui-observations").last();
        checks.equals("Recovery attempts render in one collapsed observation group", 1, group.count());
        checks.isFalse("The recovery group starts collapsed",
                (Boolean) group.evaluate("element => element.open"));
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Expand all nodes")).click();
        checks.isFalse("Expand all nodes preserves the observation disclosure state",
                (Boolean) group.evaluate("element => element.open"));
        var step = detail.locator("a.node").filter(new Locator.FilterOptions().setHasText(stepTitle)).last();
        checks.isTrue("The recovery outcome is visible at the parent step", step.isVisible());
        var summary = group.locator("summary");
        checks.isTrue("The collapsed summary identifies the attempt count",
                summary.innerText().contains(Integer.toString(records.size())));
        summary.click();

        var rows = group.locator("table.atf-ui-action-table tbody tr");
        checks.equals("Expanding reveals every recovery attempt", records.size(), rows.count());
        for (int index = 0; index < records.size(); index++) {
            var cells = rows.nth(index).locator("td");
            checks.equals("Attempt order and name are preserved: " + (index + 1),
                    records.get(index).operation(), cells.nth(0).locator(".atf-ui-action-operation").innerText());
            checks.equals("Policy source and budget appear once: " + (index + 1),
                    records.get(index).expected(), cells.nth(2).innerText());
            checks.equals("Complete decision, signal, reason, and timing appear once: " + (index + 1),
                    records.get(index).observed(), cells.nth(3).innerText());
            checks.equals("Recovery observation remains informational: " + (index + 1), "Info",
                    cells.nth(4).locator(".atf-ui-action-result").getAttribute("aria-label"));
        }
        summary.click();
        checks.isFalse("Recovery observations collapse after inspection",
                (Boolean) group.evaluate("element => element.open"));
        checks.isFalse("Collapsing hides every recovery attempt", rows.first().isVisible());
    }
}
