package atf.autoex.tests.extension;

import atf.console.junit.AtfConsoleTestContext;
import atf.console.junit.AtfConsoleTestExtension;
import atf.console.junit.AtfReportPathOptIn;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.channel.AtfTestChannelResolver;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiEvidencePlan;
import atf.testkit.evidence.AtfUiEvidenceResolver;
import atf.testkit.files.AtfTestResourceReader;
import atf.testkit.lifecycle.TestRunInitialization;
import atf.testkit.output.ReportOutputDirectory;
import atf.ui.playwright.PlaywrightUiSession;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.reporting.AutoExReportSystemMetadata;
import atf.autoex.runtime.config.AutoExProjectRoot;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static atf.reporting.ui.contract.ReportUiContract.Phase.TEST_BODY;

/** Resolves JUnit metadata before acquiring runtime resources through the shared Kit. */
final class AutoExRunFactory {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(AutoExRunFactory.class);
    private static final String SUITE_REPORT_KEY = "auto-ex-suite-report";
    private AutoExRunFactory() { }
    static AutoExE2eRun open(ExtensionContext context) {
        AutoExConfiguration configuration = AutoExConfigurationLoader.load();
        var recovery = atf.testkit.recovery.AtfUiRecoveryResolver.resolve(context.getRequiredTestClass(), context.getRequiredTestMethod());
        configuration = configuration.withRecovery(configuration.browser().recovery().restrict(
                recovery.permitsConfiguredRecovery(), recovery.source()));
        TestChannel channel = AtfTestChannelResolver.resolve(context.getRequiredTestClass())
                .orElseThrow(() -> new IllegalStateException("AutoEx E2E must declare @AtfTestChannel"))
                .channel();
        if (channel != TestChannel.UI && channel != TestChannel.UI_API) {
            throw new IllegalStateException("AutoEx purchase journey supports UI or UI_API channels only");
        }
        AtfUiEvidencePlan declaration = AtfUiEvidenceResolver.resolve(context.getRequiredTestClass())
                .orElseGet(AtfUiEvidencePlan::configured);
        var reportConfiguration = AtfReportConfigurationLoader.load();
        ReportEvidenceProfile evidence = AutoExEvidenceProfileAdapter.forRun(
                declaration, configuration.evidence().profile(reportConfiguration.screenshotPolicy()));
        ReportEvidenceProfile sessionEvidence = ReportEvidenceProfile.forScreenshotPolicy(
                reportConfiguration.screenshotPolicy());
        if (channel == TestChannel.UI_API) {
            AutoExNetworkDocuments documentSelection = context.getRequiredTestClass()
                    .getAnnotation(AutoExNetworkDocuments.class);
            configuration = AutoExE2eRun.withNetworkInspection(configuration,
                    documentSelection == null ? List.of() : Arrays.asList(documentSelection.value()));
        }
        var projectRoot = AutoExProjectRoot.discover();
        var uiBase = AtfTestResourceReader.resolveContained(
                projectRoot, configuration.uiReportBaseDirectory());
        var output = ReportOutputDirectory.resolve(projectRoot, uiBase,
                context.getRequiredTestClass());
        var console = AtfConsoleTestContext.configuredSink();
        var environment = AutoExReportSystemMetadata.snapshot(configuration, evidence, channel == TestChannel.UI_API,
                System.getProperty("atf.environment", AutoExReportSystemMetadata.DEFAULT_ENVIRONMENT));
        ReportResource reportResource = context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent(SUITE_REPORT_KEY,
                        ignored -> new ReportResource(output,
                                AutoExReportSession.fileName(context.getRequiredTestMethod().getName()), context.getRequiredTestClass(),
                                sessionEvidence, environment),
                        ReportResource.class);
        reportResource.include(context.getRequiredTestClass(), environment);
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
        private final AtfReportPathOptIn paths = new AtfReportPathOptIn(AtfConsoleTestExtension.reportPathPublisher());
        private ReportResource(Path output, String name, Class<?> owner,
                               ReportEvidenceProfile evidence, ReportEnvironmentSnapshot environment) {
            paths.include(owner);
            session = AutoExReportSession.openWithEnvironment(output, name, paths, evidence, environment);
        }
        private void include(Class<?> owner, ReportEnvironmentSnapshot environment) {
            paths.include(owner);
            session.report().includeScenarioEnvironment(environment);
        }
        private UiExtentReportSession report() { return session.report(); }
        @Override public void close() { session.close(); }
    }
}
