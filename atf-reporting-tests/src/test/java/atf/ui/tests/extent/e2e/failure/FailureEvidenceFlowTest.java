package atf.ui.tests.extent.e2e.failure;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.reporting.ui.UiAuditLifecycle;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.ui.tests.extent.support.audit.ReportAuditInputs;
import atf.ui.tests.extent.support.audit.ReportUiActionAdapters;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.UiScreenValidation;
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
import java.util.List;

import static atf.reporting.audit.ReportAuditChecks.check;
import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controlled failure-path audit. The JUnit method passes because the failure
 * is intentional, while the generated Extent audit contains a real FAIL
 * scenario with the executed actions and the YAML-controlled screenshot.
 * This package is deliberately separate from happy-path exploratory flows.
 */
@ApiScenarioSetup(families = {"users", "posts"})
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Extent failure evidence flow")
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
@ReportBrowserTest
class FailureEvidenceFlowTest {
    private UiConfiguration configuration;
    private Path inputReport;
    private Path auditReport;
    private ReportAuditContext auditContext;
    private PlaywrightActionSequence actions;

    @BeforeAll
    void openConfiguredReport() {
        configuration = ReportingTestResources.uiConfiguration(getClass());
        var input = ReportAuditInputs.configuredOrGenerated(getClass(), configuration);
        inputReport = input.report();
        assertTrue(Files.isRegularFile(inputReport),
                "Configured Extent report does not exist: " + inputReport);
        auditContext = ReportAuditContext.open(configuration, input, "failure-flow");
        auditReport = auditContext.reportPath();
    }

    @AfterAll
    void closeAudit() {
        if (auditContext != null) auditContext.close();
    }

    @Test
    @DisplayName("GIVEN a controlled UI failure WHEN the failure boundary closes THEN actions and policy screenshot are auditable")
    void failureEvidenceUsesConfiguredScreenshotPolicy() throws Exception {
        actions = auditContext.newActionSequence();
        UiExtentReportSession.UiScenario scenario = auditContext.startScenario(
                "FailureEvidenceFlowTest", "atf.ui.tests.extent.e2e.failure",
                "Controlled failure evidence", List.of("ui", "failure", "diagnostic"));
        String inputReportName = inputReport.getFileName().toString();
        boolean inputReportExists = Files.isRegularFile(inputReport);
        var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
        setup.setup(GIVEN, UiAuditGherkin.REPORT_INPUT_READY, inputReportName,
                List.of(check("Input report", "published HTML", inputReportName,
                        inputReportExists)), AtfReportStatus.PASS);
        setup.complete(AtfReportStatus.PASS);

        var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        UiAuditLifecycle lifecycle = new UiAuditLifecycle(scenario, body);
        auditContext.openTab(ReportUiContract.Tab.TESTS.hash(), actions);
        actions.observation("Failure-path probe", "#test-view",
                "a controlled failure is captured at the current report state", "intentional failure");
        UiScreenValidation.Evidence screenshot = auditContext.captureFailure(
                configuration, "Controlled UI failure");
        boolean capturedFromTestsTab = auditContext.isTabSelected(ReportUiContract.Tab.TESTS);
        lifecycle.fail("Controlled UI failure", "#test-view", ReportUiActionAdapters.from(actions.drain()),
                new AssertionError("intentional failure for failure-evidence contract"),
                screenshot.hasScreenshotPayload() ? screenshot.base64Png() : null);

        auditContext.close();
        auditContext = null;
        String html = Files.readString(auditReport);
        boolean failureRendered = html.contains("Controlled UI failure")
                && html.contains("atf-ui-action-result-fail");
        boolean screenshotPolicyRendered = configuration.browser().screenshotOnFailure()
                ? screenshot.hasScreenshotPayload() && html.contains("data:image/png;base64,")
                : !screenshot.hasScreenshotPayload();
        assertTrue(failureRendered, "The controlled FAIL scenario is not present in the audit artifact");
        assertTrue(screenshotPolicyRendered,
                "The failure screenshot does not follow ui.screenshotOnFailure");
        assertTrue(capturedFromTestsTab, "Failure evidence was captured from the wrong tab");
    }
}


