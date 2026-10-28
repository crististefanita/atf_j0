package atf.console.junit;
import atf.console.AtfConsoleConfigurationLoader;
import atf.console.AtfConsoleReporter;
import atf.console.ConsoleSettings;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.console.AtfConsoleMessagePolicyResolver;
import atf.testkit.reporting.AtfReportPath;
import atf.testkit.reporting.AtfReportPathPolicyResolver;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.TestFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Adds the configured ATF console lifecycle to directly executed JUnit tests. */
public final class AtfConsoleTestExtension implements BeforeEachCallback, TestWatcher {
    private final ConcurrentMap<String, AtfConsoleTestLifecycle> activeTests =
            new ConcurrentHashMap<>();

    @Override
    public void beforeEach(ExtensionContext context) {
        if (context.getRequiredTestMethod().isAnnotationPresent(TestFactory.class)) {
            // A factory is a dynamic-test container, not an executable test result.
            // Each child opts into a correctly scoped lifecycle via AtfConsoleTestContext.dynamicTest(...).
            return;
        }
        var lifecycle = lifecycle(context.getRequiredTestClass(),
                context.getRequiredTestClass().getSimpleName(),
                context.getRequiredTestMethod().getName());
        lifecycle.start();
        activeTests.put(context.getUniqueId(), lifecycle);
        AtfConsoleTestContext.install(lifecycle);
    }

    static ConsoleSettings effectiveSettings(Class<?> testClass) {
        ConsoleSettings configured = AtfConsoleConfigurationLoader.load().settings();
        return applyPolicy(configured, AtfConsoleMessagePolicyResolver.resolve(testClass));
    }

    /** Lifecycle messages and final-path notifications share an owner, not a display policy. */
    static AtfConsoleTestLifecycle lifecycle(Class<?> owner, String scenario, String test) {
        ConsoleSettings configured = AtfConsoleConfigurationLoader.load().settings();
        var messages = AtfConsoleReporter.configuredStdout(
                applyPolicy(configured, AtfConsoleMessagePolicyResolver.resolve(owner)));
        var paths = AtfConsoleReporter.configuredStdout(configured, reportPathEnabled(owner));
        return new AtfConsoleTestLifecycle(event -> {
            if (event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED) paths.publish(event);
            else messages.publish(event);
        }, scenario, test);
    }

    /** Resolves report-path opt-in independently from {@code @AtfConsoleMessages}. */
    public static boolean reportPathEnabled(Class<?> testClass) {
        return AtfReportPathPolicyResolver.resolve(testClass) == AtfReportPath.Policy.ENABLED;
    }

    /** Sink for a per-class report owner; emits only the final-path event. */
    public static AtfConsoleSink reportPathSink(Class<?> testClass) {
        var reporter = AtfConsoleReporter.configuredStdout(
                AtfConsoleConfigurationLoader.load().settings(), reportPathEnabled(testClass));
        return event -> {
            if (event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED) {
                reporter.publish(event);
            }
        };
    }

    /** Destination for a shared scope that aggregates opt-in across its report owners. */
    public static AtfConsoleSink reportPathPublisher() {
        var reporter = AtfConsoleReporter.configuredStdout(
                AtfConsoleConfigurationLoader.load().settings(), true);
        return event -> {
            if (event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED) {
                reporter.publish(event);
            }
        };
    }

    static ConsoleSettings applyPolicy(ConsoleSettings configured, AtfConsoleMessages.Policy policy) {
        return switch (policy) {
            case INHERIT -> configured;
            case DETAILS -> new ConsoleSettings(configured.enabled(), true);
            case SUMMARY -> new ConsoleSettings(configured.enabled(), false);
            case OFF -> ConsoleSettings.disabled();
        };
    }

    @Override public void testSuccessful(ExtensionContext context) { complete(context, AtfReportStatus.PASS); }
    @Override public void testFailed(ExtensionContext context, Throwable cause) { complete(context, AtfReportStatus.FAIL); }
    @Override public void testAborted(ExtensionContext context, Throwable cause) { complete(context, AtfReportStatus.SKIP); }

    private void complete(ExtensionContext context, AtfReportStatus status) {
        var lifecycle = activeTests.remove(context.getUniqueId());
        if (lifecycle != null) lifecycle.complete(status);
        AtfConsoleTestContext.clear();
    }
}
