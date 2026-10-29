package atf.reporting.ui.timing;

import atf.reporting.model.timing.ReportPhaseEvidenceTiming;
import atf.reporting.model.timing.ReportPhaseTiming;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Caller starts/ends the phase; Reporting separates capture and publication without inferring elapsed work. */
public final class UiPhaseExecution {
    private final LongSupplier ticks;
    private final long started;
    private long captureNanos;
    private long publicationNanos;
    private long excludedNanos;
    private Duration execution;
    private boolean recording;
    private ReportPhaseTiming completed;

    public UiPhaseExecution(LongSupplier ticks) {
        this.ticks = Objects.requireNonNull(ticks, "ticks");
        started = ticks.getAsLong();
    }

    public <T> T capture(Supplier<T> work) { return measure(work, true); }
    public void publish(Runnable work) { measure(() -> { work.run(); return null; }, false); }

    /** Stops business execution before final evidence, cleanup, resource release and report flush. */
    public void finishExecution() {
        if (recording) throw new IllegalStateException("Cannot finish execution during evidence work");
        if (execution == null) execution = Duration.ofNanos(Math.subtractExact(elapsed(started), excludedNanos));
    }

    public ReportPhaseTiming snapshot() {
        if (completed != null) return completed;
        finishExecution();
        return new ReportPhaseTiming(execution, 1, Optional.of(new ReportPhaseEvidenceTiming(
                Duration.ofNanos(captureNanos), Duration.ofNanos(publicationNanos))));
    }

    /** Freezes all axes after the terminal evidence has been published. */
    public ReportPhaseTiming complete() {
        if (completed == null) completed = snapshot();
        return completed;
    }

    private <T> T measure(Supplier<T> work, boolean capture) {
        Objects.requireNonNull(work, "work");
        if (completed != null) throw new IllegalStateException("UI phase measurement is already complete");
        if (recording) throw new IllegalStateException("Evidence measurements must not overlap");
        long start = ticks.getAsLong();
        recording = true;
        Throwable primary = null;
        try { return work.get(); }
        catch (RuntimeException | Error failure) { primary = failure; throw failure; }
        finally {
            recording = false;
            try {
                long elapsed = elapsed(start);
                if (execution == null) excludedNanos = Math.addExact(excludedNanos, elapsed);
                if (capture) captureNanos = Math.addExact(captureNanos, elapsed);
                else publicationNanos = Math.addExact(publicationNanos, elapsed);
            } catch (RuntimeException | Error timingFailure) {
                if (primary == null) throw timingFailure;
                if (primary != timingFailure) primary.addSuppressed(timingFailure);
            }
        }
    }

    private long elapsed(long start) {
        long value = ticks.getAsLong() - start;
        if (value < 0) throw new IllegalStateException("Monotonic phase clock moved backwards");
        return value;
    }
}
