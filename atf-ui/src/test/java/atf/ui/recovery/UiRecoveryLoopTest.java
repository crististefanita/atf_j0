package atf.ui.recovery;

import atf.testkit.AtfUnitTest;
import atf.ui.config.recovery.UiRecoveryConfiguration;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static atf.ui.recovery.UiRecoveryTrace.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UiRecoveryLoopTest {
    private static final Instant BASE = Instant.parse("2026-10-28T12:00:00Z");
    private static final UiRecoveryOperation OP = new UiRecoveryOperation("catalog-readiness", UiRecoveryOperation.Safety.READ_ONLY,
            URI.create("http://localhost/"), URI.create("http://localhost/catalog"), "GET");
    private static final class Fixture implements UiRecoveryLoop.Observation {
        final AtomicLong nanos = new AtomicLong();
        final List<UiRecoveryTrace> traces = new ArrayList<>();
        final UiRecoverySignal.Stamp stamp = new UiRecoverySignal.Stamp(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0);
        Optional<UiRecoverySignal> signal = Optional.empty();
        Throwable failure;
        boolean current = true, conflict;
        final Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return BASE.plusNanos(nanos.get()); }
        };
        UiRecoveryLoop loop(boolean enabled, boolean capture, int attempts, Duration timeout, UiRecoveryLoop.Wait wait) {
            return new UiRecoveryLoop(new UiRecoveryConfiguration(enabled, "read-only", Set.of(OP.name()), attempts,
                    timeout, Duration.ofMillis(10), capture, "YAML; fixture"), OP, stamp, nanos::get, clock, wait, traces::add);
        }
        UiRecoveryLoop loop(int attempts) { return loop(true, true, attempts, Duration.ofSeconds(3), d -> nanos.addAndGet(d.toNanos())); }
        void response(int status, String retry) { signal = Optional.of(new UiRecoverySignal(stamp, 1, clock.instant(), UiRecoverySignal.Kind.RESPONSE, status, retry)); }
        public boolean currentDocument() { return current; }
        public boolean conflictingRequest() { return conflict; }
        public Optional<UiRecoverySignal> signal() { return signal; }
        public Optional<Throwable> failure() { return Optional.ofNullable(failure); }
    }
    @Test void aSuccessfulInitialProbeNeedsNeitherRecoveryNorPassiveCaptureWhenOff() {
        var f = new Fixture(); var loop = f.loop(false, false, 1, Duration.ofSeconds(1), d -> fail("must not wait"));
        assertEquals(Decision.INITIAL_READY, loop.await(f, () -> Dom.READY).decision());
        assertEquals(1, f.traces.size()); assertTrue(f.traces.getFirst().signal().isEmpty());
        assertThrows(IllegalStateException.class, () -> loop.await(f, () -> Dom.READY));
    }
    @Test void correlatedSuccessWithDelayedDomReobservesWithoutAnActionCallback() {
        var f = new Fixture(); var loop = f.loop(3); f.response(200, "");
        var calls = new AtomicInteger();
        var result = loop.await(f, () -> { f.nanos.addAndGet(5_000_000); return calls.incrementAndGet() == 1 ? Dom.PENDING : Dom.READY; });
        assertEquals(Decision.RECOVERED, result.decision()); assertEquals(2, calls.get());
        assertEquals(Duration.ofMillis(20), result.totalTime());
        assertEquals(Duration.ofMillis(5), result.probeTime());
        assertEquals(Duration.ofMillis(10), f.traces.getFirst().backoffTime());
    }
    @Test void aCorrelatedTransientResponseMustBecomeSuccessfulBeforeRecoveredDomCanPass() {
        var f = new Fixture(); var loop = f.loop(3); f.response(503, "");
        var calls = new AtomicInteger();
        assertEquals(Decision.RECOVERED, loop.await(f, () -> {
            if (calls.incrementAndGet() == 2) f.response(200, ""); return Dom.READY;
        }).decision());
        assertEquals(2, calls.get()); assertEquals(Reason.TRANSIENT, f.traces.getFirst().reason());
    }
    @Test void refusalMatrixDoesNotPromoteMissingForeignFailedRedirectOrBusinessSignals() {
        for (var reason : List.of(Reason.MISSING_SIGNAL, Reason.UNCORRELATED_SIGNAL, Reason.REQUEST_FAILED,
                Reason.HTTP_REFUSAL, Reason.REDIRECT_UNCONFIRMED, Reason.CONFLICTING_REQUEST,
                Reason.BUSINESS_INVALID, Reason.SESSION_MISMATCH, Reason.AMBIGUOUS, Reason.DOCUMENT_CHANGED)) {
            var f = new Fixture(); var loop = f.loop(3); f.response(200, "");
            Dom dom = Dom.PENDING;
            switch (reason) {
                case MISSING_SIGNAL -> f.signal = Optional.empty();
                case UNCORRELATED_SIGNAL -> f.signal = Optional.of(new UiRecoverySignal(
                        new UiRecoverySignal.Stamp(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0), 1, BASE, UiRecoverySignal.Kind.RESPONSE, 503, ""));
                case REQUEST_FAILED -> f.signal = Optional.of(new UiRecoverySignal(f.stamp, 1, BASE, UiRecoverySignal.Kind.REQUEST_FAILED, 0, ""));
                case HTTP_REFUSAL -> f.response(400, "");
                case REDIRECT_UNCONFIRMED -> f.response(302, "");
                case CONFLICTING_REQUEST -> f.conflict = true;
                case DOCUMENT_CHANGED -> f.current = false;
                case BUSINESS_INVALID -> dom = Dom.BUSINESS_INVALID;
                case SESSION_MISMATCH -> dom = Dom.SESSION_MISMATCH;
                case AMBIGUOUS -> dom = Dom.AMBIGUOUS;
                default -> throw new AssertionError(reason);
            }
            Dom state = dom;
            assertEquals(reason, assertThrows(UiRecoveryException.class, () -> loop.await(f, () -> state)).trace().reason());
            assertEquals(1, f.traces.size()); assertFalse(f.traces.stream().anyMatch(t -> t.decision() == Decision.REOBSERVE));
        }
    }
    @Test void offAndUncapturedPoliciesCannotBorrowUiApiPresentationAsPermission() {
        var off = new Fixture(); off.response(503, "");
        assertEquals(Reason.DISABLED, assertThrows(UiRecoveryException.class,
                () -> off.loop(false, true, 3, Duration.ofSeconds(1), d -> fail()).await(off, () -> Dom.PENDING)).trace().reason());
        var missing = new Fixture(); missing.response(503, "");
        assertEquals(Reason.CAPTURE_DISABLED, assertThrows(UiRecoveryException.class,
                () -> missing.loop(true, false, 3, Duration.ofSeconds(1), d -> fail()).await(missing, () -> Dom.PENDING)).trace().reason());
    }
    @Test void firstEvaluationConsumesAnAttemptAndLateSuccessCannotEscapeTheSharedDeadline() {
        var f = new Fixture(); var loop = f.loop(2); f.response(200, "");
        var count = new AtomicInteger();
        var failure = assertThrows(UiRecoveryException.class, () -> loop.await(f, () -> { count.incrementAndGet(); return Dom.PENDING; }));
        assertEquals(2, count.get()); assertEquals(Reason.ATTEMPTS, failure.trace().reason());
        var late = new Fixture(); var lateLoop = late.loop(3); late.response(200, "");
        assertEquals(Reason.BUDGET, assertThrows(UiRecoveryException.class, () -> lateLoop.await(late,
                () -> { late.nanos.addAndGet(Duration.ofSeconds(4).toNanos()); return Dom.READY; })).trace().reason());
        var actionSpent = new Fixture(); var spentLoop = actionSpent.loop(3);
        actionSpent.nanos.set(Duration.ofSeconds(4).toNanos());
        assertEquals(0, assertThrows(UiRecoveryException.class, () -> spentLoop.await(actionSpent, () -> { fail("no probe budget remains"); return Dom.READY; })).trace().attempt());
    }
    @Test void retryAfterIsNotRenewedAndCannotExceedTheOneDeadline() {
        var f = new Fixture(); var loop = f.loop(3); f.response(429, "1");
        assertThrows(UiRecoveryException.class, () -> loop.await(f, () -> Dom.PENDING));
        assertEquals(Duration.ofSeconds(1), f.traces.getFirst().backoffTime());
        assertEquals(Duration.ofMillis(10), f.traces.get(1).backoffTime());
        var over = new Fixture(); var shortLoop = over.loop(3); over.response(503, "60");
        assertEquals(Reason.BUDGET, assertThrows(UiRecoveryException.class, () -> shortLoop.await(over, () -> Dom.PENDING)).trace().reason());
        assertEquals(0, over.nanos.get());
        var invalid = new Fixture(); var invalidLoop = invalid.loop(3); invalid.response(503, "-1");
        assertEquals(Reason.INVALID_RETRY_AFTER, assertThrows(UiRecoveryException.class, () -> invalidLoop.await(invalid, () -> Dom.PENDING)).trace().reason());
    }
    @Test void captureFailurePreservesItsCauseAndNeverReobserves() {
        var f = new Fixture(); var loop = f.loop(3); f.response(200, "");
        var primary = new IllegalStateException("controlled capture failure");
        f.failure = primary;
        assertSame(primary, assertThrows(IllegalStateException.class, () -> loop.await(f, () -> Dom.READY)));
        assertEquals(Reason.CAPTURE_FAILURE, f.traces.getFirst().reason());
        assertEquals(1, f.traces.size()); assertEquals(0, f.nanos.get());
    }
    @Test void publicationFaultCannotRetryBusinessOrMaskItsPrimaryFailure() {
        var f = new Fixture(); f.response(200, "");
        var publication = new IllegalStateException("controlled publication failure");
        var policy = new UiRecoveryConfiguration(true, "read-only", Set.of(OP.name()), 3,
                Duration.ofSeconds(3), Duration.ofMillis(10), true, "YAML");
        var loop = new UiRecoveryLoop(policy, OP, f.stamp, f.nanos::get, f.clock, d -> fail("no wait"),
                trace -> { throw publication; });
        var business = new AssertionError("original business mismatch");
        assertSame(business, assertThrows(AssertionError.class, () -> loop.await(f, () -> { throw business; })));
        assertEquals(List.of(publication), List.of(business.getSuppressed()));
        assertEquals(0, f.nanos.get());
    }
    @Test void businessAssertionAndInterruptionAreNeverRetriedOrRewritten() {
        var f = new Fixture(); var loop = f.loop(3); f.response(200, "");
        var original = new AssertionError("business mismatch");
        assertSame(original, assertThrows(AssertionError.class, () -> loop.await(f, () -> { throw original; })));
        assertEquals(Reason.PROBE_FAILURE, f.traces.getFirst().reason());
        var stop = new Fixture(); stop.response(200, "");
        try {
            assertThrows(java.util.concurrent.CancellationException.class,
                    () -> stop.loop(true, true, 3, Duration.ofSeconds(1), d -> { throw new InterruptedException(); }).await(stop, () -> Dom.PENDING));
            assertTrue(Thread.currentThread().isInterrupted()); assertEquals(Decision.ABORTED, stop.traces.getFirst().decision());
        } finally { Thread.interrupted(); }
    }
}
