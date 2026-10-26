package atf.ui.playwright;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.ui.AtfUiActionRecord;
import atf.core.ui.AtfUiEvidenceSource;
import atf.ui.config.UiRetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Owns ordered UI evidence, sequence boundaries and operation timing. */
final class UiActionRecorder {
    private final List<AtfUiActionRecord> records = new ArrayList<>();
    private final UiActionTrace trace;
    private UiRetryPolicy retryPolicy = UiRetryPolicy.none();
    private int nextSequence = 1;

    UiActionRecorder() {
        this(UiActionTrace.noOp());
    }

    UiActionRecorder(UiActionTrace trace) {
        this.trace = trace == null ? UiActionTrace.noOp() : trace;
    }

    void setRetryPolicy(UiRetryPolicy policy) {
        retryPolicy = policy == null ? UiRetryPolicy.none() : policy;
    }

    <T> T run(String operation, String target, String expected, Action<T> action) {
        return run(operation, target, expected, false, action);
    }

    <T> T run(String operation, String target, String expected, boolean sensitive, Action<T> action) {
        return run(operation, target, expected, sensitive, action,
                observed -> observed == null ? "operation completed" : observed.toString());
    }

    <T> T run(String operation, String target, String expected, boolean sensitive, Action<T> action,
              Function<T, String> safeObservation) {
        long started = System.nanoTime();
        String safeExpected = expected;
        publish(new UiActionTrace.UiActionEvent(UiActionTrace.Kind.START,
                operation, target, safeExpected, "", null, 0));
        int attempt = 1;
        while (true) {
            try {
                T observed = action.run();
                String value = Objects.requireNonNull(safeObservation.apply(observed), "safeObservation result");
                long duration = elapsedMillis(started);
                add(operation, target, safeExpected, value, duration, AtfReportStatus.PASS);
                publish(new UiActionTrace.UiActionEvent(UiActionTrace.Kind.COMPLETE,
                        operation, target, safeExpected, value, AtfReportStatus.PASS, duration));
                return observed;
            } catch (RuntimeException | AssertionError failure) {
                if (retryPolicy.allows(operation, attempt)) {
                    int nextAttempt = attempt + 1;
                    String failureObserved = failureDescription(failure, sensitive);
                    String attemptExpected = "attempt " + attempt + " failed; retrying as attempt "
                            + nextAttempt + " of " + retryPolicy.maxAttempts();
                    add("Retry attempt", target, attemptExpected, failureObserved,
                            elapsedMillis(started), AtfReportStatus.INFO);
                    publish(new UiActionTrace.UiActionEvent(UiActionTrace.Kind.OBSERVATION,
                            "Retry attempt", target, attemptExpected, failureObserved,
                            AtfReportStatus.INFO, elapsedMillis(started)));
                    String retryExpected = "attempt " + nextAttempt + " of " + retryPolicy.maxAttempts();
                    String retryObserved = "retry scheduled: " + retryPolicy.reason();
                    add("Retry", target, retryExpected, retryObserved, 0, AtfReportStatus.INFO);
                    publish(new UiActionTrace.UiActionEvent(UiActionTrace.Kind.OBSERVATION,
                            "Retry", target, retryExpected, retryObserved, AtfReportStatus.INFO, 0));
                    retryPolicy.pause();
                    attempt = nextAttempt;
                    continue;
                }
                String value = failureDescription(failure, sensitive);
                long duration = elapsedMillis(started);
                add(operation, target, safeExpected, value, duration, AtfReportStatus.FAIL);
                publish(new UiActionTrace.UiActionEvent(UiActionTrace.Kind.COMPLETE,
                        operation, target, safeExpected, value, AtfReportStatus.FAIL, duration));
                if (sensitive) throwSafeFailure(failure, value);
                throw failure;
            }
        }
    }

    private static String failureDescription(Throwable failure, boolean sensitive) {
        if (sensitive) return "Sensitive UI action failed (" + failure.getClass().getSimpleName() + ")";
        return failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }

    private static void throwSafeFailure(Throwable failure, String safeMessage) {
        if (failure instanceof AssertionError) throw new AssertionError(safeMessage);
        throw new IllegalStateException(safeMessage);
    }

    void observation(String operation, String target, String expected, String observed) {
        add(operation, target, expected, observed, 0, AtfReportStatus.PASS,
                AtfUiEvidenceSource.OBSERVE);
        publish(new UiActionTrace.UiActionEvent(UiActionTrace.Kind.OBSERVATION,
                operation, target, expected, observed, AtfReportStatus.PASS, 0));
    }

    List<AtfUiActionRecord> snapshot() {
        return List.copyOf(records);
    }

    List<AtfUiActionRecord> drain() {
        List<AtfUiActionRecord> result = snapshot();
        records.clear();
        return result;
    }

    private void add(String operation, String target, String expected, String observed,
                     long durationMillis, AtfReportStatus status) {
        records.add(new AtfUiActionRecord(nextSequence++, operation, target, expected,
                observed, durationMillis, status, AtfUiEvidenceSource.ACTION));
    }

    private void add(String operation, String target, String expected, String observed,
                     long durationMillis, AtfReportStatus status, AtfUiEvidenceSource source) {
        records.add(new AtfUiActionRecord(nextSequence++, operation, target, expected,
                observed, durationMillis, status, source));
    }

    private static long elapsedMillis(long started) {
        return Math.max(1L, (System.nanoTime() - started + 999_999L) / 1_000_000L);
    }

    private void publish(UiActionTrace.UiActionEvent event) {
        try {
            trace.publish(event);
        } catch (RuntimeException ignored) {
            // Diagnostic telemetry is best-effort and must never change a UI verdict.
        }
    }

    @FunctionalInterface
    interface Action<T> {
        T run();
    }
}
