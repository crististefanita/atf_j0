package atf.console.junit;
import atf.console.AtfConsoleReporter;
import atf.console.ConsoleSettings;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Reusable console lifecycle for a directly executed JUnit API test. */
public final class AtfConsoleTestLifecycle {
    private final AtfConsoleSink console;
    private final String scenarioName;
    private final String testName;
    private final LongSupplier nanoClock;
    private long startedAt;
    private boolean started;
    private boolean completed;
    private AtfReportStatus observedStatus = AtfReportStatus.INFO;
    private long validationStartedAt;

    public AtfConsoleTestLifecycle(AtfConsoleSink console, String scenarioName, String testName) {
        this(console, scenarioName, testName, System::nanoTime);
    }

    AtfConsoleTestLifecycle(AtfConsoleSink console, String scenarioName, String testName,
                            LongSupplier nanoClock) {
        this.console = Objects.requireNonNull(console, "console");
        this.scenarioName = requireText(scenarioName, "scenarioName");
        this.testName = requireText(testName, "testName");
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    /** Creates a lifecycle using the discovered {@code atf.yaml}. */
    public static AtfConsoleTestLifecycle configured(String scenarioName, String testName) {
        return new AtfConsoleTestLifecycle(
                AtfConsoleReporter.configuredStdout(), scenarioName, testName);
    }

    /** Creates a lifecycle using the supplied effective policy over the configured process console. */
    public static AtfConsoleTestLifecycle configured(String scenarioName, String testName,
                                                      ConsoleSettings settings) {
        return new AtfConsoleTestLifecycle(
                AtfConsoleReporter.configuredStdout(settings), scenarioName, testName);
    }

    /** Creates a lifecycle with an independently resolved report-path policy. */
    public static AtfConsoleTestLifecycle configured(String scenarioName, String testName,
                                                      ConsoleSettings settings,
                                                      boolean reportPathEnabled) {
        return new AtfConsoleTestLifecycle(
                AtfConsoleReporter.configuredStdout(settings, reportPathEnabled), scenarioName, testName);
    }

    public void start() {
        if (started || completed) return;
        startedAt = nanoClock.getAsLong();
        started = true;
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.SCENARIO, scenarioName));
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.PHASE,
                "TEST BODY - " + testName));
    }

    public void complete(AtfReportStatus status) {
        Objects.requireNonNull(status, "status");
        if (!started || completed) return;
        completed = true;
        AtfReportStatus effectiveStatus = AtfReportStatus.worst(observedStatus, status);
        long durationMillis = Math.max(0, (nanoClock.getAsLong() - startedAt) / 1_000_000);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.PHASE,
                "TEST BODY - " + testName, effectiveStatus, durationMillis));
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.SCENARIO,
                scenarioName, effectiveStatus, durationMillis));
    }

    public void recordStatus(AtfReportStatus status) {
        if (!completed) {
            observedStatus = AtfReportStatus.worst(observedStatus,
                    Objects.requireNonNull(status, "status"));
        }
    }

    public void detail(String name, String value) {
        if (started && !completed) {
            console.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, name, value));
        }
    }

    public void code(String name, String value) {
        if (started && !completed) {
            console.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST, name, value));
        }
    }

    public AtfConsoleSink console() { return console; }

    public void startValidation(String title) {
        if (started && !completed) {
            validationStartedAt = nanoClock.getAsLong();
            console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.VALIDATION, title));
        }
    }

    public void completeValidation(String title, AtfReportStatus status) {
        if (started && !completed) {
            long duration = Math.max(0, (nanoClock.getAsLong() - validationStartedAt) / 1_000_000);
            console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.VALIDATION,
                    title, status, duration));
        }
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
