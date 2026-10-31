package atf.ui.tests.extent.support.inspection.reconciliation;

import atf.reporting.support.failurecorrelation.UiFailureCorrelationFixture;
import atf.reporting.support.navigation.FailureLinkIdentity;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import java.nio.file.Path;
import java.util.List;
import static atf.reporting.contract.AtfReportStatus.PASS;

import static atf.reporting.audit.ReportAuditChecks.check;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises real Fails projection and replay links of the current generated report. */
public final class UiFailureCorrelationProbe {
    private static final ReportUiLocators LOCATORS = new ReportUiLocators();
    private UiFailureCorrelationProbe() { }

    public static void verify(Path report) {
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, "ui-failure-correlation-audit")) {
            var audit = new UiAuditFlow(context, policy, UiFailureCorrelationFixture.OWNER,
                    UiFailureCorrelationProbe.class.getPackageName(), List.of("component", "ui"),
                    "failure correlation input exists", "failure correlation audit failed", "correlation");
            var seen = new int[UiFailureCorrelationFixture.SCENARIOS.size()];
            var mixedOwners = new java.util.HashSet<String>();
            for (int ordinal = 0; ordinal < UiFailureCorrelationFixture.FAILURE_COUNT; ordinal++) {
                int index = ordinal;
                audit.audit("failure " + (index + 1) + " retains its exact source", inspected -> {
                    var page = audit.openTab(ReportUiContract.Tab.FAILS.hash());
                    var fails = LOCATORS.failsView(page);
                    assertEquals(UiFailureCorrelationFixture.FAILURE_COUNT, LOCATORS.failureItems(fails).count());
                    audit.actions().click(LOCATORS.failureItemAction(page, index), "inspect failure cause");
                    var detail = LOCATORS.failureDetail(fails);
                    String text = detail.innerText();
                    int scenario = java.util.stream.IntStream.range(0, seen.length)
                            .filter(candidate -> text.contains(UiFailureCorrelationFixture.SCENARIOS.get(candidate)))
                            .findFirst().orElseThrow();
                    seen[scenario]++;
                    if (scenario == 5) {
                        String markup = detail.innerHTML();
                        mixedOwners.add(markup.contains("data-atf-evidence-owner=\"api\"") ? "api" : "ui");
                    }
                    if (scenario == 0 || scenario == 1 || scenario == 5 || scenario == 7) {
                        assertTrue(!text.contains("ASSERTION\nFailure cause"), text);
                    }
                    String anchor = "scenario-index=" + scenario;
                    assertTrue(FailureLinkIdentity.parse(LOCATORS.failureDetailOpenLink(fails).getAttribute("href"))
                            .hasParameter("scenario-index", Integer.toString(scenario)), "Backlink retains its exact scenario parameter");
                    audit.actions().click(LOCATORS.failureDetailOpenAction(page), "open original failing scenario");
                    audit.actions().waitForVisible(LOCATORS.testsView(page), ReportUiContract.Tab.TESTS.hash());
                    var original = LOCATORS.scenarios(LOCATORS.visibleTestDetail(page)).nth(scenario);
                    audit.actions().waitForAttribute(page, LOCATORS.nodeCardAction(original),
                            "data-atf-scenario-open", "true", "exact failure scenario is open");
                    assertEquals("true", original.getAttribute("data-atf-scenario-open"));
                    new ApiReportUiFlow(audit, LOCATORS).expandScenario(original);
                    assertTrue(original.innerText().contains("Terminal diagnostics"));
                    assertTrue(original.innerText().contains("Confirmation visible"));
                    inspected.screen("failure maps to its original checkpoint", "Tests / " + scenario,
                            audit.capture(page, "Failure correlation replay"), List.of(
                                    check("Original scenario", anchor, page.url(),
                                            FailureLinkIdentity.parse(page.url()).hasParameter("scenario-index", Integer.toString(scenario)))),
                            PASS);
                });
            }
            assertEquals(java.util.Set.of("ui", "api"), mixedOwners,
                    "the same checkpoint retains both independent channels");
            for (int index = 0; index < seen.length; index++) {
                assertEquals(UiFailureCorrelationFixture.FAILURES.get(index).intValue(), seen[index],
                        UiFailureCorrelationFixture.SCENARIOS.get(index));
            }
            assertTrue(context.browserDiagnostics().valid(), context.browserDiagnostics().violations().toString());
        }
    }
}
