package atf.autoex.tests.extension;

import atf.console.junit.AtfConsoleTestContext;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.channel.AtfTestChannelResolver;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.testkit.files.AtfTestResourceReader;
import atf.testkit.lifecycle.TestRunInitialization;
import atf.testkit.output.ReportOutputDirectory;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.config.AutoExProjectRoot;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.List;

import static atf.reporting.ui.contract.ReportUiContract.Phase.TEST_BODY;

/** Resolves JUnit metadata before acquiring runtime resources through the shared Kit. */
final class AutoExRunFactory {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AutoExRunFactory.class);
    private static final String SUITE_REPORT_KEY = "auto-ex-suite-report";
    private AutoExRunFactory() { }
    static AutoExE2eRun open(ExtensionContext context) {
        AutoExConfiguration configuration = AutoExConfigurationLoader.load();
        TestChannel channel = AtfTestChannelResolver.resolve(context.getRequiredTestClass())
                .orElseThrow(() -> new IllegalStateException("AutoEx E2E must declare @AtfTestChannel"))
                .channel();
        if (channel != TestChannel.UI && channel != TestChannel.UI_API) {
            throw new IllegalStateException("AutoEx purchase journey supports UI or UI_API channels only");
        }
        AtfUiEvidence evidenceAnnotation = context.getRequiredTestClass()
                .getAnnotation(AtfUiEvidence.class);
        AtfUiEvidenceFlow flow = evidenceAnnotation == null
                ? AtfUiEvidenceFlow.CONFIGURED : evidenceAnnotation.flow();
        AtfUiScreenshotPolicy screenshots = evidenceAnnotation == null
                ? AtfUiScreenshotPolicy.CONFIGURED : evidenceAnnotation.screenshots();
        var reportConfiguration = AtfReportConfigurationLoader.load();
        ReportEvidenceProfile evidence = AutoExEvidenceProfileAdapter.forRun(
                flow, screenshots, reportConfiguration.screenshotPolicy());
        ReportEvidenceProfile sessionEvidence = ReportEvidenceProfile.forScreenshotPolicy(
                reportConfiguration.screenshotPolicy());
        if (channel == TestChannel.UI_API) {
            configuration = AutoExE2eRun.withNetworkInspection(configuration);
        }
        var projectRoot = AutoExProjectRoot.discover();
        var uiBase = AtfTestResourceReader.resolveContained(
                projectRoot, configuration.uiReportBaseDirectory());
        var output = ReportOutputDirectory.resolve(projectRoot, uiBase,
                context.getRequiredTestClass());
        var console = AtfConsoleTestContext.configuredSink();
        UiBrowserConfiguration reportBrowser = configuration.browser();
        ReportResource reportResource = context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent(SUITE_REPORT_KEY,
                        ignored -> new ReportResource(output,
                                AutoExReportSession.fileName(context.getRequiredTestMethod().getName()), console,
                                sessionEvidence, reportBrowser),
                        ReportResource.class);
        UiExtentReportSession report = reportResource.report();
        AutoExConfiguration runConfiguration = configuration;
        return TestRunInitialization.create(
                () -> PlaywrightUiSession.open(runConfiguration.browser(), console),
                browser -> report.startScenario(context.getRequiredTestClass().getSimpleName(),
                        context.getRequiredTestClass().getPackageName(), context.getDisplayName(),
                        List.of("e2e", "ui", "automation-exercise"), evidence),
                scenario -> scenario.startPhase(TEST_BODY.displayName()),
                (browser, scenario, phase) -> new AutoExE2eRun(runConfiguration, browser, scenario, phase, evidence,
                        channel == TestChannel.UI_API, AutoExRunLifecycleAdapter.INSTANCE),
                phase -> phase.complete(AtfReportStatus.FAIL),
                scenario -> scenario.complete(AtfReportStatus.FAIL),
                PlaywrightUiSession::close);
    }

    private static final class ReportResource implements ExtensionContext.Store.CloseableResource {
        private final AutoExReportSession session;
        private ReportResource(Path output, String name, AtfConsoleSink console,
                               ReportEvidenceProfile evidence, UiBrowserConfiguration browser) {
            session = AutoExReportSession.open(output, name, console, evidence, browser);
        }
        private UiExtentReportSession report() { return session.report(); }
        @Override public void close() { session.close(); }
    }
}
