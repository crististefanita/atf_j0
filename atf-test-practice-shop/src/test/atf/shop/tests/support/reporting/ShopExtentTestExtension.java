package atf.shop.tests.support.reporting;

import atf.shop.support.reporting.component.ShopComponentReportFactory;
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

import java.util.List;

/** JUnit metadata and result projection; Kit owns the resource scope and Reporting owns the session. */
public final class ShopExtentTestExtension implements ComponentAuditAdapter {
    private final ComponentAuditExtension audit = ComponentAuditExtension.scoped(
            ShopExtentTestExtension.class, Owner.class, ShopExtentTestExtension::openOwner, ShopExtentTestExtension::open);
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
        var run = ShopComponentReportFactory.open(sink);
        return new Owner(run, paths);
    }

    private static ComponentAuditExtension.Publication open(Owner owner, ExtensionContext context) {
        var type = context.getRequiredTestClass();
        owner.paths().include(type);
        String method = context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("Class lifecycle");
        String title = context.getTestMethod().isPresent() ? context.getDisplayName() : "Class lifecycle: " + type.getSimpleName();
        var report = owner.run().open(new AssertionAuditReport.Identity(type.getSimpleName(),
                type.getPackageName(), title, method, List.of("component", "practice-shop")));
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
