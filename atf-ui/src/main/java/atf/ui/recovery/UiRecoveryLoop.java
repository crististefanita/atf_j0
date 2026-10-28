package atf.ui.recovery;

import atf.ui.config.recovery.UiRecoveryConfiguration;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.*;
import static atf.ui.recovery.UiRecoveryTrace.*;

/** One monotonic deadline starts before the original action; this API deliberately has no action/replay callback. */
public final class UiRecoveryLoop {
    public interface Observation {
        boolean currentDocument();
        boolean conflictingRequest();
        Optional<UiRecoverySignal> signal();
        default Optional<Throwable> failure() { return Optional.empty(); }
    }
    @FunctionalInterface public interface Wait { void pause(Duration delay) throws InterruptedException; }
    private final UiRecoveryConfiguration policy;
    private final UiRecoveryOperation operation;
    private final UiRecoverySignal.Stamp stamp;
    private final Instant capturedAt;
    private final LongSupplier ticks;
    private final Clock wallClock;
    private final Wait wait;
    private final Consumer<UiRecoveryTrace> sink;
    private final long started;
    private boolean evaluated;

    public UiRecoveryLoop(UiRecoveryConfiguration policy, UiRecoveryOperation operation, UiRecoverySignal.Stamp stamp,
                           LongSupplier ticks, Clock wallClock, Wait wait, Consumer<UiRecoveryTrace> sink) {
        this.policy = Objects.requireNonNull(policy); this.operation = Objects.requireNonNull(operation);
        this.stamp = Objects.requireNonNull(stamp); this.ticks = Objects.requireNonNull(ticks);
        this.wallClock = Objects.requireNonNull(wallClock); this.wait = Objects.requireNonNull(wait); this.sink = Objects.requireNonNull(sink);
        started = ticks.getAsLong(); capturedAt = wallClock.instant();
    }
    public Duration remaining() {
        return Duration.ofNanos(Math.max(0, policy.totalTimeout().toNanos() - elapsed().toNanos()));
    }
    public UiRecoveryTrace await(Observation observation, Supplier<Dom> probe) {
        if (evaluated) throw new IllegalStateException("A recovery scope is single-use");
        evaluated = true;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            if (Thread.currentThread().isInterrupted()) throw interrupted(attempt - 1, Duration.ZERO, Duration.ZERO, observation.signal());
            if (policy.enabled() && remaining().isZero()) throw terminal(attempt - 1, Decision.EXHAUSTED, Reason.BUDGET, Duration.ZERO, observation.signal());
            if (!observation.currentDocument()) throw terminal(attempt - 1, Decision.REFUSED, Reason.DOCUMENT_CHANGED, Duration.ZERO, observation.signal());
            long probeStart = ticks.getAsLong();
            Dom dom;
            try { dom = Objects.requireNonNull(probe.get(), "readiness probe"); }
            catch (RuntimeException | Error failure) {
                var trace = trace(attempt, Decision.REFUSED, Reason.PROBE_FAILURE, since(probeStart), Duration.ZERO, observation.signal());
                try { sink.accept(trace); } catch (RuntimeException | Error secondary) { if (secondary != failure) failure.addSuppressed(secondary); }
                throw failure;
            }
            Duration measured = since(probeStart);
            var signal = observation.signal();
            if (observation.failure().isPresent()) {
                var failure = observation.failure().orElseThrow();
                try { sink.accept(trace(attempt, Decision.REFUSED, Reason.CAPTURE_FAILURE, measured, Duration.ZERO, signal)); }
                catch (RuntimeException | Error secondary) { if (secondary != failure) failure.addSuppressed(secondary); }
                if (failure instanceof Error error) throw error;
                if (failure instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("Backend metadata capture failed", failure);
            }
            if (policy.enabled() && remaining().isZero()) throw terminal(attempt, Decision.EXHAUSTED, Reason.BUDGET, measured, signal);
            if (!observation.currentDocument()) throw terminal(attempt, Decision.REFUSED, Reason.DOCUMENT_CHANGED, measured, signal);
            if (dom != Dom.READY && dom != Dom.PENDING) throw terminal(attempt, Decision.REFUSED, Reason.valueOf(dom.name()), measured, signal);
            if (attempt == 1 && dom == Dom.READY && (!policy.enabled() || !policy.captureBackendSignals())) {
                return emit(trace(attempt, Decision.INITIAL_READY, Reason.READY, measured, Duration.ZERO, Optional.empty()));
            }
            if (!policy.enabled()) throw terminal(attempt, Decision.REFUSED, Reason.DISABLED, measured, signal);
            if (operation.safety() != UiRecoveryOperation.Safety.READ_ONLY || !policy.operations().contains(operation.name())) {
                throw terminal(attempt, Decision.REFUSED, Reason.OPERATION_NOT_ALLOWED, measured, signal);
            }
            if (!policy.captureBackendSignals()) throw terminal(attempt, Decision.REFUSED, Reason.CAPTURE_DISABLED, measured, signal);
            if (observation.conflictingRequest()) throw terminal(attempt, Decision.REFUSED, Reason.CONFLICTING_REQUEST, measured, signal);
            if (signal.isEmpty()) throw terminal(attempt, Decision.REFUSED, Reason.MISSING_SIGNAL, measured, signal);
            var receipt = signal.get();
            if (!stamp.equals(receipt.stamp()) || receipt.sequence() < 1 || receipt.capturedAt().isBefore(capturedAt)) {
                throw terminal(attempt, Decision.REFUSED, Reason.UNCORRELATED_SIGNAL, measured, signal);
            }
            if (receipt.kind() == UiRecoverySignal.Kind.REQUEST_FAILED) throw terminal(attempt, Decision.REFUSED, Reason.REQUEST_FAILED, measured, signal);
            int status = receipt.status();
            if (receipt.kind() == UiRecoverySignal.Kind.RESPONSE) {
                if (status >= 300 && status < 400) throw terminal(attempt, Decision.REFUSED, Reason.REDIRECT_UNCONFIRMED, measured, signal);
                if (!(status >= 200 && status < 300) && !Set.of(429, 502, 503, 504).contains(status)) {
                    throw terminal(attempt, Decision.REFUSED, Reason.HTTP_REFUSAL, measured, signal);
                }
                if (status >= 200 && status < 300 && dom == Dom.READY) {
                    return emit(trace(attempt, attempt == 1 ? Decision.INITIAL_READY : Decision.RECOVERED, Reason.READY, measured, Duration.ZERO, signal));
                }
            }
            if (attempt == policy.maxAttempts()) throw terminal(attempt, Decision.EXHAUSTED, Reason.ATTEMPTS, measured, signal);
            Duration delay;
            try { delay = max(policy.backoff(), retryAfter(receipt)); }
            catch (RuntimeException invalid) { throw terminal(attempt, Decision.REFUSED, Reason.INVALID_RETRY_AFTER, measured, signal); }
            if (delay.compareTo(remaining()) >= 0) throw terminal(attempt, Decision.EXHAUSTED, Reason.BUDGET, measured, signal);
            long pauseStart = ticks.getAsLong();
            try { wait.pause(delay); }
            catch (InterruptedException stopped) { Thread.currentThread().interrupt(); throw interrupted(attempt, measured, since(pauseStart), signal); }
            emit(trace(attempt, Decision.REOBSERVE, status >= 200 && status < 300 ? Reason.DOM_PENDING : Reason.TRANSIENT,
                    measured, since(pauseStart), signal));
        }
        throw new IllegalStateException("Recovery attempt accounting failed");
    }
    private Duration retryAfter(UiRecoverySignal signal) {
        if (signal.kind() != UiRecoverySignal.Kind.RESPONSE || !Set.of(429, 502, 503, 504).contains(signal.status()) || signal.retryAfter().isEmpty()) return Duration.ZERO;
        String value = signal.retryAfter();
        Instant until = value.matches("[0-9]+") ? signal.capturedAt().plusSeconds(Long.parseLong(value))
                : ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        Duration delay = Duration.between(wallClock.instant(), until);
        return delay.isNegative() ? Duration.ZERO : delay;
    }
    private UiRecoveryTrace emit(UiRecoveryTrace trace) { sink.accept(trace); return trace; }
    private UiRecoveryException terminal(int attempt, Decision decision, Reason reason, Duration probe, Optional<UiRecoverySignal> signal) {
        var trace = trace(attempt, decision, reason, probe, Duration.ZERO, signal);
        var failure = new UiRecoveryException(trace);
        try { sink.accept(trace); } catch (RuntimeException | Error secondary) { failure.addSuppressed(secondary); }
        return failure;
    }
    private CancellationException interrupted(int attempt, Duration probe, Duration backoff, Optional<UiRecoverySignal> signal) {
        var failure = new CancellationException("BE recovery observation interrupted; no action replay");
        try { emit(trace(attempt, Decision.ABORTED, Reason.INTERRUPTED, probe, backoff, signal)); }
        catch (RuntimeException | Error secondary) { failure.addSuppressed(secondary); }
        return failure;
    }
    private UiRecoveryTrace trace(int attempt, Decision decision, Reason reason, Duration probe, Duration backoff, Optional<UiRecoverySignal> signal) {
        return new UiRecoveryTrace(operation.name(), attempt, decision, reason, signal, probe, backoff, elapsed(), policy.source());
    }
    private Duration elapsed() { return since(started); }
    private Duration since(long start) {
        long nanos = ticks.getAsLong() - start;
        if (nanos < 0) throw new IllegalStateException("Recovery monotonic clock moved backwards");
        return Duration.ofNanos(nanos);
    }
    private static Duration max(Duration a, Duration b) { return a.compareTo(b) >= 0 ? a : b; }
}
