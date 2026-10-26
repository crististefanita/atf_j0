package atf.reporting.api.tests.support.junit;

import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.extent.session.ExtentReportSession;
import atf.console.junit.AtfConsoleTestExtension;
import atf.reporting.api.tests.support.console.AtfConsoleBridge;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;

/**
 * Opens one configured Extent artifact for a directly executed resource test
 * class. The extension is deliberately test-layer code: production modules
 * do not depend on JUnit and the API composition root does not depend on HTML.
 */
public final class AtfExtentReportTestExtension implements BeforeAllCallback,
        BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(AtfExtentReportTestExtension.class);
    private static final String SESSION = "session";
    private static final String TEST = "test";

    @Override
    public void beforeAll(ExtensionContext context) {
        var config = AtfReportConfigurationLoader.load();
        if (config.enabled()) {
            context.getStore(NAMESPACE).put(SESSION,
                    ExtentReportSession.openConfigured(config, environment(context),
                            AtfConsoleBridge.toReportingSink(AtfConsoleTestExtension.reportPathSink(
                                    context.getRequiredTestClass()))));
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var session = context.getStore(NAMESPACE).get(SESSION, ExtentReportSession.class);
        if (session == null) return;

        String className = context.getRequiredTestClass().getSimpleName();
        String scenario = context.getDisplayName();
        String testPackage = context.getRequiredTestClass().getPackageName();
        String resource = resourceTag(context.getRequiredTestClass().getPackageName());
        var report = ApiReportTestContext.open(session, className, testPackage, scenario,
                List.of("api", resource, "contract"));
        context.getStore(NAMESPACE).put(TEST, report);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var report = context.getStore(NAMESPACE).remove(TEST, ApiReportTestContext.class);
        if (report == null) return;
        var status = context.getExecutionException().isPresent()
                ? AtfReportStatus.FAIL : AtfReportStatus.PASS;
        report.complete(status);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var session = context.getStore(NAMESPACE).remove(SESSION, ExtentReportSession.class);
        if (session != null) session.close();
    }

    private static String resourceTag(String packageName) {
        if (packageName.endsWith(".users")) return "users";
        if (packageName.endsWith(".posts")) return "posts";
        if (packageName.endsWith(".lifecycle")) return "lifecycle";
        return "api";
    }

    private static ReportEnvironmentSnapshot environment(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        String resource = resourceTag(testClass.getPackageName());
        return ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "ACCEPTANCE")
                .put(ReportEnvironmentField.TEST_PACKAGE, testClass.getPackageName())
                .put(ReportEnvironmentField.SUITE, "API contract tests")
                .put(ReportEnvironmentField.PROFILE, resource)
                .put(ReportEnvironmentField.TRANSPORT_MODE, "ACCEPTANCE")
                .put(ReportEnvironmentField.PRECONDITION, "atf-mock.yaml")
                .put(ReportEnvironmentField.FLOW, "API request/response contract")
                .put(ReportEnvironmentField.FIXTURE, "atf-reporting-tests")
                .put(ReportEnvironmentField.API_EVIDENCE, "request + response + execution")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "API contract artifact")
                .build();
    }
}
