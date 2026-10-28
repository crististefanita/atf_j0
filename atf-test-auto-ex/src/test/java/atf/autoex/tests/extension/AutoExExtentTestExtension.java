package atf.autoex.tests.extension;

import atf.autoex.runtime.config.AutoExProjectRoot;
import atf.testkit.output.ReportOutputDirectory;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.ComponentAssertionAudit;
import atf.testkit.assertions.ComponentAuditAdapter;
import atf.testkit.assertions.ComponentAuditExtension;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import org.junit.jupiter.api.extension.ExtensionContext;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Consumer-owned configuration and projection over the composed Kit invocation protocol. */
public final class AutoExExtentTestExtension implements ComponentAuditAdapter {
    private final ComponentAuditExtension audit = new ComponentAuditExtension(AutoExExtentTestExtension.class, AutoExExtentTestExtension::open);
    @Override public ComponentAuditExtension auditExtension() { return audit; }

    private static ComponentAuditExtension.Publication open(ExtensionContext context) throws Exception {
        var output = ReportOutputDirectory.resolveProjectRelative(AutoExProjectRoot.discover(), "artifacts/reports/component/auto-ex", "artifacts/reports/component/auto-ex");
        Files.createDirectories(output);
        var type = context.getRequiredTestClass();
        String method = context.getRequiredTestMethod().getName();
        String name = type.getSimpleName() + "_" + method + "_"
                + Instant.now().toString().replace(":", "-") + "_" + UUID.randomUUID().toString().substring(0, 8) + ".html";
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "COMPONENT")
                .put(ReportEnvironmentField.SUITE, "Automation Exercise component")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Per-test result").build();
        var redactor = new SensitiveDataRedactor();
        var session = new ExtentReportSession(output.resolve(name), AtfReportConfigurationLoader.load().reportingSettings(),
                redactor, atf.reporting.console.AtfConsoleSink.noOp(), environment);
        var report = new AssertionAuditReport(session, new AssertionAuditReport.Identity(type.getSimpleName(),
                type.getPackageName(), context.getDisplayName(), method, List.of("component", "auto-ex")), redactor);
        return new ComponentAuditExtension.Publication() {
            @Override public AssertionValueDisplay display() {
                return new AssertionValueDisplay(AssertionValueDisplay.DEFAULT_LIMIT, report::redactField);
            }
            @Override public void publish(ComponentAssertionAudit.Result result) {
                for (var phase : result.phases()) report.phase(phase.phase().name().replace('_', ' '),
                        phase.checks().stream().map(check -> new AtfAssertion(check.name(), check.expected(),
                                check.actual(), check.passed())).toList(), status(phase.outcome()), phase.diagnostic());
                report.complete(status(result.outcome()));
            }
            @Override public void close() { report.close(); }
        };
    }

    private static AtfReportStatus status(ComponentAssertionAudit.Outcome outcome) {
        return switch (outcome) {
            case PASS -> AtfReportStatus.PASS;
            case FAIL -> AtfReportStatus.FAIL;
            case ABORTED -> AtfReportStatus.SKIP;
        };
    }
}
