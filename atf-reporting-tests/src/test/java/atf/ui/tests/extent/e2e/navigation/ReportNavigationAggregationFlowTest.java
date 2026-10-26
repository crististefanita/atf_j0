package atf.ui.tests.extent.e2e.navigation;
import atf.reporting.contract.AtfAssertion;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.ui.tests.extent.support.navigation.ReportNavigationFlow;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;

import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.ui.tests.extent.support.audit.ReportAuditInputs;
import atf.ui.playwright.UiScreenValidation;
import atf.reporting.ui.audit.section.OverviewSectionValidator;
import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.reporting.audit.ReportAuditChecks.check;
import static atf.reporting.audit.ReportAuditChecks.status;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gherkin intent only; selector mechanics are delegated to {@link ReportNavigationFlow}. */
@ApiScenarioSetup(families = {"users", "posts"})
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Extent report navigation and aggregation flow")
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
class ReportNavigationAggregationFlowTest {
    private UiConfiguration configuration;
    private Path inputReport;
    private Path auditReport;
    private ReportAuditContext auditContext;
    private UiAuditFlow auditFlow;
    private ReportNavigationFlow reportFlow;

    @BeforeAll
    void openConfiguredReport() {
        configuration = ReportingTestResources.uiConfiguration(getClass());
        var input = ReportAuditInputs.configuredOrGenerated(getClass(), configuration);
        inputReport = input.report();
        assertTrue(Files.isRegularFile(inputReport), "Configured Extent report does not exist: " + inputReport);
        auditContext = ReportAuditContext.open(configuration, input, "navigation-flow");
        auditFlow = new UiAuditFlow(auditContext, configuration,
                "ReportNavigationAggregationFlowTest", "atf.ui.tests.extent.component.architecture",
                List.of("ui", "flow", "navigation", "aggregation"),
                UiAuditGherkin.REPORT_INPUT_READY,
                "Navigation flow audit failure", "selected or freshly generated API Extent report");
        reportFlow = new ReportNavigationFlow(auditFlow);
        auditReport = auditContext.reportPath();
    }

    @AfterAll
    void closeAudit() { if (auditContext != null) auditContext.close(); }

    @Test
    @DisplayName("GIVEN a report WHEN Tests, filters, Categories and Overview are traversed THEN the artifact remains consistent")
    void testsFiltersCategoriesAndOverviewReconcile() {
        audit("Tests → status/tag filters → Clear → Categories → Overview", audit -> {
            ReportNavigationFlow.NavigationSnapshot snapshot = reportFlow.traverse();
            UiScreenValidation.Evidence evidence = auditFlow.capture(snapshot.overviewPage(),
                    "Tests, filters, Categories and Overview reconciled");
            var baseline = snapshot.baseline();
            var category = snapshot.category();
            var overview = new OverviewSectionValidator().validate(snapshot.overview(), baseline,
                    new OverviewSectionValidator.Expected(List.of(), 0, java.util.Set.of()));
            boolean valid = snapshot.passSelected() && snapshot.passScenarioCount() >= 0
                    && snapshot.combinedScenarioCount() >= 0 && snapshot.tagCombinationValid()
                    && snapshot.clearRestoresBaseline() && category.valid() && overview.valid()
                    && evidence.hasScreenshotPayload();
            List<AtfAssertion> assertions = new ArrayList<>(overview.assertions());
            assertions.add(0, check("Tests baseline", "owners/scenarios > 0", baseline.classCount() + "/" + baseline.scenarioCount(), baseline.classCount() > 0 && baseline.scenarioCount() > 0));
            assertions.add(check("Pass filter", "selected and non-negative", snapshot.passSelected() + " / " + snapshot.passScenarioCount(), snapshot.passSelected() && snapshot.passScenarioCount() >= 0));
            assertions.add(check("Dynamic tag combination",
                    "second tag replaces first; one active tag remains",
                    "'" + snapshot.firstTagName() + "' (" + snapshot.firstTagScenarioCount()
                            + " visible) → '" + snapshot.secondTagName() + "'"
                            + ", active=" + snapshot.activeTagCount()
                            + ", replaced=" + snapshot.firstTagReplaced()
                            + ", selections=" + snapshot.tagSelectionSteps(),
                    snapshot.tagCombinationValid()));
            assertions.add(check("Clear",
                    "owners=" + baseline.classCount() + ", status=Clear, tags=0",
                    "owners=" + snapshot.clearedOwnerCount()
                            + ", status=" + (snapshot.statusClearSelected() ? "Clear" : "active")
                            + ", tags=" + snapshot.activeTagCountAfterClear()
                            + ", selected-owner scenarios=" + snapshot.clearedScenarioCount(),
                    snapshot.clearRestoresBaseline()));
            assertions.add(check("Categories", "selected category has coherent rows/links", category.toString(), category.valid()));
            assertions.add(check("Inline Base64 screenshot", "valid PNG payload", String.valueOf(evidence.hasScreenshotPayload()), evidence.hasScreenshotPayload()));
            audit.screen("Overview reconciles the unfiltered Tests baseline after filter exploration",
                    "#test-view → #category-view → #dashboard-view", evidence, assertions, status(valid));
            assertAll(() -> assertTrue(snapshot.passSelected()), () -> assertTrue(snapshot.tagCombinationValid()),
                    () -> assertTrue(snapshot.clearRestoresBaseline()), () -> assertTrue(category.valid()),
                    () -> assertTrue(overview.valid(), overview.failureSummary()));
        });
    }

    private void audit(String title, UiAuditFlow.AuditAction action) { auditFlow.audit(title, action); }
}
