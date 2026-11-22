package atf.autoex.tests.support.reporting;

import atf.core.ui.AtfUiActionRecord;
import atf.testkit.assertions.RecordedAssertions;
import atf.ui.recovery.UiRecoveryTrace;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.time.Duration;
import java.util.regex.Pattern;
import java.time.format.DateTimeParseException;

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
            checks.equals("Semantic recovery operation remains the action target: " + (index + 1),
                    records.get(index).target(), cells.nth(1).innerText());
            checks.equals("Policy source and budget appear once: " + (index + 1),
                    records.get(index).expected(), cells.nth(2).innerText());
            checks.equals("Complete decision, signal, reason, and timing appear once: " + (index + 1),
                    records.get(index).observed(), cells.nth(3).innerText());
            checks.equals("Recovery observation remains informational: " + (index + 1), "Info",
                    cells.nth(4).locator(".atf-ui-action-result").getAttribute("aria-label"));
        }
    }

    public static void verifyActualComponentReport(Page page, Path reportPath, UiRecoveryTrace.Decision outcome,
                                                   Path screenshotPath, RecordedAssertions checks) throws IOException {
        String html = Files.readString(reportPath);
        checks.isFalse("Actual report excludes the private response body",
                html.contains("backend-private-body-must-not-be-captured"));
        checks.isFalse("Actual report excludes the local fixture address", html.contains("127.0.0.1"));

        page.navigate(reportPath.toUri().toString());
        page.locator("#nav-test").click();
        var owner = page.locator("p.name:text-is('AutoExBackendRecoveryComponentTest')");
        checks.equals("The actual JUnit report identifies the recovery component owner", 1, owner.count());
        owner.locator("xpath=ancestor::li[contains(concat(' ', normalize-space(@class), ' '), ' test-item ')][1]")
                .click();

        var detail = page.locator("#test-view .test-content-detail .detail-body");
        var title = "Backend recovery trace · " + outcome;
        var group = detail.locator("details.atf-observation-evidence")
                .filter(new Locator.FilterOptions().setHasText(title)).last();
        checks.equals("The actual component report contains the requested recovery outcome", 1, group.count());
        checks.isFalse("The actual recovery observation starts collapsed",
                (Boolean) group.evaluate("element => element.open"));
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Expand all nodes")).click();
        checks.isFalse("Expand all nodes preserves the recovery disclosure state",
                (Boolean) group.evaluate("element => element.open"));
        var phaseCandidates = group.locator("xpath=ancestor::div[contains(@class, 'card')]")
                .filter(new Locator.FilterOptions().setHasText("TEST BODY"));
        var phase = phaseCandidates.last();
        var componentResult = phase.locator("xpath=ancestor::div[contains(@class, 'card')][1]");
        var resultNode = componentResult.locator("xpath=./div[contains(@class, 'card-header')]//a[contains(@class, 'node')]");
        var bodyNode = phase.locator("xpath=./div[contains(@class, 'card-header')]//a[contains(@class, 'node') and normalize-space(.)='TEST BODY']");
        checks.isTrue("The actual recovery trace remains inside a TEST BODY phase", phaseCandidates.count() > 0);
        checks.equals("The actual recovery trace remains under its component result", 1, resultNode.count());
        checks.equals("The actual recovery trace remains inside TEST BODY", 1, bodyNode.count());
        if (!bodyNode.isVisible()) resultNode.click();
        checks.isTrue("Expanding the component result exposes TEST BODY", bodyNode.isVisible());
        var assertionNode = phase.locator("a.node")
                .filter(new Locator.FilterOptions().setHasText("Component assertions"));
        checks.equals("TEST BODY exposes its Component assertions child", 1, assertionNode.count());
        checks.isTrue("The component result exposes its assertion step", assertionNode.isVisible());
        var phaseContent = phase.locator(":scope > .collapse");
        if (!phaseContent.isVisible()) bodyNode.click();
        checks.isTrue("Expanding TEST BODY exposes its phase content", phaseContent.isVisible());
        checks.isTrue("Expanded TEST BODY exposes the recovery trace", group.isVisible());
        var assertions = phase.locator("table.helm-validation-table tbody tr");
        if (!assertions.first().isVisible()) assertionNode.click();
        checks.isTrue("Expanding the assertion step reveals all assertion rows",
                assertions.count() > 0 && assertions.first().isVisible());
        for (int index = 0; index < assertions.count(); index++) {
            checks.isTrue("Component assertion remains PASS: " + (index + 1),
                    assertions.nth(index).locator("td").nth(3).innerText().contains("✅"));
        }
        group.locator("summary").click();

        var rows = group.locator("table.helm-evidence-table tbody tr");
        checks.isTrue("The actual trace has complete attempt rows", rows.count() >= 6 && rows.count() % 6 == 0);
        int attempts = rows.count() / 6;
        String terminalDecision = "";
        for (int attempt = 1; attempt <= attempts; attempt++) {
            var operation = fieldValue(rows, attempt, "OPERATION");
            var event = fieldValue(rows, attempt, "EVENT");
            var observation = fieldValue(rows, attempt, "OBSERVATION");
            var timing = fieldValue(rows, attempt, "TIMING");
            var result = fieldValue(rows, attempt, "RESULT");
            checks.equals("Semantic operation is visible in the actual report: " + attempt,
                    "catalog-readiness", operation);
            checks.equals("Event order is visible in the actual report: " + attempt,
                    "Backend observation #" + attempt, event);
            checks.isTrue("Observation includes a typed decision and reason: " + attempt,
                    observation.contains(";") && observation.contains("no business action replay"));
            checks.isTrue("Three measured timings parse as ISO-8601 durations: " + attempt,
                    hasTypedTimings(timing));
            checks.equals("Each actual recovery row remains INFO: " + attempt, "INFO", result);
            if (attempt == attempts) terminalDecision = observation.substring(0, observation.indexOf(';'));
        }
        checks.equals("Actual trace ends in the selected outcome", outcome.name(), terminalDecision);
        group.scrollIntoViewIfNeeded();
        Files.createDirectories(screenshotPath.toAbsolutePath().getParent());
        page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
        checks.isTrue("Actual report viewport capture was written", Files.size(screenshotPath) > 0);
    }

    private static String fieldValue(Locator rows, int attempt, String field) {
        String name = ("ATTEMPT " + attempt + " · " + field).toUpperCase(Locale.ROOT);
        var matches = rows.filter(new Locator.FilterOptions().setHasText(name));
        if (matches.count() != 1) throw new AssertionError("Expected one rendered field: " + name);
        return matches.locator("td").nth(1).innerText();
    }

    private static boolean hasTypedTimings(String value) {
        var pattern = Pattern.compile("(probe|backoff|total)=(PT[^;]+)");
        var matcher = pattern.matcher(value);
        int count = 0;
        while (matcher.find()) {
            try {
                Duration.parse(matcher.group(2));
            } catch (DateTimeParseException invalid) {
                return false;
            }
            count++;
        }
        return count == 3;
    }
}
