package atf.autoex.tests.extension;

import atf.autoex.runtime.component.AutoExComponentReportFactory;
import atf.console.junit.AtfConsoleTestExtension;
import atf.console.junit.AtfReportPathOptIn;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.ComponentAssertionAudit;
import atf.testkit.assertions.ComponentAuditAdapter;
import atf.testkit.assertions.ComponentAuditExtension;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.extension.ExtensionContext;
import atf.autoex.runtime.config.AutoExProjectRoot;
import atf.testkit.output.ReportOutputDirectory;
import java.util.List;

/** JUnit metadata and result projection; Kit owns the resource scope and Reporting owns the session. */
public final class AutoExExtentTestExtension implements ComponentAuditAdapter {
    public static final String REPORT_DIRECTORY_PROPERTY = "atf.autoex.component.report-directory";
    private final ComponentAuditExtension audit = ComponentAuditExtension.scoped(
            AutoExExtentTestExtension.class, Owner.class, AutoExExtentTestExtension::openOwner, AutoExExtentTestExtension::open);
    @Override public ComponentAuditExtension auditExtension() { return audit; }

    private record Owner(AssertionAuditRun run, AtfReportPathOptIn paths) implements AutoCloseable {
        @Override public void close() { run.close(); }
    }

    private static Owner openOwner(ExtensionContext context) {
        var paths = new AtfReportPathOptIn(AtfConsoleTestExtension.reportPathPublisher());
        paths.include(context.getRequiredTestClass());
        atf.reporting.console.AtfConsoleSink sink = event -> paths.publish(AtfConsoleEvent.fromExternalEnums(
                event.type(), event.scope(), event.name(), event.status(), event.durationMillis(),
                event.detailName(), event.detailValue()));
        var output = ReportOutputDirectory.resolveProjectRelative(AutoExProjectRoot.discover(),
                System.getProperty(REPORT_DIRECTORY_PROPERTY, "artifacts/reports/component/auto-ex"),
                "artifacts/reports/component/auto-ex");
        var run = AutoExComponentReportFactory.open(output, sink);
        return new Owner(run, paths);
    }

    private static ComponentAuditExtension.Publication open(Owner owner, ExtensionContext context) {
        var type = context.getRequiredTestClass();
        owner.paths().include(type);
        String method = context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("Class lifecycle");
        String title = context.getTestMethod().isPresent() ? context.getDisplayName() : "Class lifecycle: " + type.getSimpleName();
        var report = owner.run().open(new AssertionAuditReport.Identity(type.getSimpleName(),
                type.getPackageName(), title, method, List.of("component", "auto-ex")));
        return new ComponentAuditExtension.Publication() {
            @Override public AssertionValueDisplay display() {
                return new AssertionValueDisplay(owner.run().assertionMaxDisplayCharacters(), report::redactField);
            }
            @Override public void publish(ComponentAssertionAudit.Result result) {
                for (var phase : result.phases()) report.phase(phase.phase().name().replace('_', ' '),
                        phase.checks().stream().map(check -> new AtfAssertion(check.name(), check.expected(),
                                check.actual(), check.passed())).toList(), status(phase.outcome()), phase.diagnostic(), phase.timing().map(value -> new atf.reporting.model.timing.ReportPhaseTiming(
                                value.elapsed(), value.intervals())).orElse(null));
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
