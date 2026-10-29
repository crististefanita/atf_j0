package atf.core.http.execution;

import atf.core.http.execution.evidence.*;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.*;
import atf.core.http.transport.*;
import atf.core.config.*;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ApiTerminalExecutionEvidenceTest {
    private static final URI ORIGIN = URI.create("https://fixture.test");
    private static final ApiRequest REQUEST = ApiRequest.of("GET", "/read");
    private static final class Clock implements ApiExecutionClock {
        long nanos;
        Runnable afterSleep = () -> { };
        boolean interruptSleep;
        public long nanoTime() { return nanos; }
        void advance(long value) { nanos += value; }
        public void sleepNanos(long value) throws InterruptedException {
            if (interruptSleep) throw new InterruptedException("controlled backoff interruption");
            advance(value); afterSleep.run();
        }
    }
    private static ApiExecutor executor(ApiTransport transport, Clock clock, Duration backoff) {
        return new ApiExecutor(transport, ORIGIN, RetryPolicy.onStatuses(Set.of(503), 2, backoff), clock);
    }
    private static ApiResponse busy(String url) { return new ApiResponse(503, "busy", Map.of(), Duration.ofMillis(3), url); }
    private static ApiExecutionSnapshot snapshot(Throwable error) { return ApiExecutionFailure.find(error).orElseThrow(); }

    @Test void aTerminalTransportFailureRetainsTheEarlier503ThroughThePublicContract() {
        var clock = new Clock(); var calls = new AtomicInteger();
        var original = new IOException("controlled terminal transport failure");
        ApiTransport transport = (request, url, auth) -> {
            clock.advance(3_000_000);
            return calls.incrementAndGet() == 1 ? CompletableFuture.completedFuture(busy(url)) : CompletableFuture.failedFuture(original);
        };
        var executor = executor(transport, clock, Duration.ofMillis(5));
        var failure = assertThrows(ApiExecutionException.class, () -> executor.execute(REQUEST));
        var evidence = failure.snapshot().orElseThrow();
        assertEquals(2, calls.get()); assertSame(original, failure.getCause()); assertSame(REQUEST, evidence.request());
        assertEquals(ORIGIN + "/read", evidence.url()); assertEquals(ApiTransportProvenance.UNKNOWN, evidence.provenance());
        assertEquals(2, evidence.attempts()); assertEquals(1, evidence.retryEvents().size());
        assertEquals(Duration.ofMillis(11), evidence.totalDuration());
        assertEquals(List.of(ApiAttemptOutcome.RETRYABLE_RESPONSE, ApiAttemptOutcome.FAILED),
                evidence.attemptHistory().stream().map(ApiAttemptRecord::outcome).toList());
        assertEquals(503, evidence.attemptHistory().getFirst().httpStatus().orElseThrow());
        assertTrue(evidence.attemptHistory().getLast().httpStatus().isEmpty()); assertTrue(evidence.response().isEmpty());
        assertEquals(Duration.ofMillis(3), evidence.attemptHistory().getLast().duration());
        assertThrows(UnsupportedOperationException.class, () -> evidence.attemptHistory().clear());
        assertThrows(UnsupportedOperationException.class, () -> evidence.retryEvents().clear());
    }

    @Test void a503FollowedByTimeoutKeepsTheDeadlineAndCancelsItsFuture() {
        var clock = new Clock(); var calls = new AtomicInteger();
        var pending = new CompletableFuture<ApiResponse>() {
            @Override public ApiResponse get(long timeout, TimeUnit unit) throws TimeoutException {
                clock.advance(unit.toNanos(timeout)); throw new TimeoutException("controlled pending response");
            }
        };
        ApiTransport transport = (request, url, auth) -> calls.incrementAndGet() == 1 ? CompletableFuture.completedFuture(busy(url)) : pending;
        var failure = assertThrows(ApiExecutionException.class,
                () -> executor(transport, clock, Duration.ofMillis(5)).execute(REQUEST, Duration.ofMillis(20), CancellationToken.none()));
        var evidence = snapshot(failure);
        assertEquals(2, calls.get()); assertTrue(pending.isCancelled()); assertInstanceOf(TimeoutException.class, failure.getCause());
        assertEquals(Duration.ofMillis(20), evidence.totalDuration()); assertEquals(ApiAttemptOutcome.TIMED_OUT, evidence.termination());
        assertEquals(2, evidence.attempts()); assertEquals(503, evidence.attemptHistory().getFirst().statusCode());
        assertTrue(evidence.attemptHistory().getLast().httpStatus().isEmpty()); assertTrue(evidence.response().isEmpty());
    }

    @Test void cancellationInBackoffRetainsOneDispatchAndThePlannedRetry() {
        var clock = new Clock(); var calls = new AtomicInteger(); var cancelled = new AtomicBoolean();
        clock.afterSleep = () -> cancelled.set(true);
        ApiTransport transport = (request, url, auth) -> { calls.incrementAndGet(); return CompletableFuture.completedFuture(busy(url)); };
        var failure = assertThrows(CancellationException.class, () -> executor(transport, clock, Duration.ofMillis(20))
                .execute(REQUEST, Duration.ofSeconds(1), cancelled::get));
        var evidence = snapshot(failure);
        assertInstanceOf(CancellationException.class, failure.getCause()); assertEquals(1, calls.get());
        assertEquals(1, evidence.attempts()); assertEquals(2, evidence.retryEvents().getFirst().attempt());
        assertEquals(ApiAttemptOutcome.CANCELLED, evidence.termination()); assertTrue(evidence.response().isEmpty());
        assertEquals(ApiAttemptOutcome.RETRYABLE_RESPONSE, evidence.attemptHistory().getFirst().outcome());
        assertEquals(Duration.ofMillis(20), evidence.totalDuration());
    }

    @Test void preDispatchCancellationDoesNotInventAnAttempt() {
        var calls = new AtomicInteger();
        ApiTransport transport = (request, url, auth) -> { calls.incrementAndGet(); return CompletableFuture.completedFuture(busy(url)); };
        var failure = assertThrows(CancellationException.class, () -> executor(transport, new Clock(), Duration.ZERO)
                .execute(REQUEST, Duration.ofSeconds(1), () -> true));
        assertEquals(0, calls.get()); assertEquals(0, snapshot(failure).attempts());
        assertTrue(snapshot(failure).attemptHistory().isEmpty()); assertTrue(snapshot(failure).response().isEmpty());
    }

    @Test void cancellationDuringTransportCancelsOnlyThePendingAttempt() {
        var clock = new Clock(); var cancelled = new AtomicBoolean(); var calls = new AtomicInteger();
        var pending = new CompletableFuture<ApiResponse>() {
            @Override public ApiResponse get(long timeout, TimeUnit unit) throws TimeoutException {
                clock.advance(1_000_000); cancelled.set(true); throw new TimeoutException("poll");
            }
        };
        ApiTransport transport = (request, url, auth) -> { calls.incrementAndGet(); return pending; };
        var failure = assertThrows(CancellationException.class, () -> executor(transport, clock, Duration.ZERO)
                .execute(REQUEST, Duration.ofSeconds(1), cancelled::get));
        assertEquals(1, calls.get()); assertTrue(pending.isCancelled());
        assertEquals(ApiAttemptOutcome.CANCELLED, snapshot(failure).attemptHistory().getFirst().outcome());
        assertTrue(snapshot(failure).attemptHistory().getFirst().httpStatus().isEmpty());
    }

    @Test void interruptionPreservesItsCauseFlagAndCancelsThePendingFuture() {
        var clock = new Clock(); var original = new InterruptedException("controlled interruption");
        var pending = new CompletableFuture<ApiResponse>() {
            @Override public ApiResponse get(long timeout, TimeUnit unit) throws InterruptedException { throw original; }
        };
        try {
            var failure = assertThrows(ApiExecutionException.class, () -> executor((request, url, auth) -> pending, clock, Duration.ZERO).execute(REQUEST));
            assertSame(original, failure.getCause()); assertTrue(Thread.currentThread().isInterrupted()); assertTrue(pending.isCancelled());
            assertEquals(ApiAttemptOutcome.INTERRUPTED, snapshot(failure).termination());
            assertEquals(1, snapshot(failure).attempts()); assertTrue(snapshot(failure).response().isEmpty());
        } finally { Thread.interrupted(); }
    }

    @Test void timeoutOrInterruptionInBackoffAddsNoPhantomAttempt() {
        for (boolean interrupted : List.of(false, true)) {
            var clock = new Clock(); clock.interruptSleep = interrupted; var calls = new AtomicInteger();
            try {
                var failure = assertThrows(ApiExecutionException.class, () -> executor((request, url, auth) -> {
                    calls.incrementAndGet(); return CompletableFuture.completedFuture(busy(url));
                }, clock, Duration.ofMillis(40)).execute(REQUEST, Duration.ofMillis(20), CancellationToken.none()));
                assertEquals(1, calls.get()); assertEquals(1, snapshot(failure).attempts());
                assertEquals(interrupted ? ApiAttemptOutcome.INTERRUPTED : ApiAttemptOutcome.TIMED_OUT, snapshot(failure).termination());
                assertEquals(interrupted, Thread.currentThread().isInterrupted());
                assertTrue(snapshot(failure).response().isEmpty());
            } finally { Thread.interrupted(); }
        }
    }

    @Test void synchronousFirstFailureAndConsoleFailurePreserveTheirOriginalCauses() {
        var primary = new IllegalArgumentException("synchronous send failed");
        var failure = assertThrows(ApiExecutionException.class, () -> executor((request, url, auth) -> { throw primary; },
                new Clock(), Duration.ZERO).execute(REQUEST));
        assertSame(primary, failure.getCause()); assertEquals(1, snapshot(failure).attempts());
        var publication = new IllegalStateException("controlled diagnostic publication failure");
        var settings = new ApiSettings(ORIGIN, "none", Duration.ofSeconds(1), Set.of(), 1);
        var reporter = new ApiExecutor((request, url, auth) -> CompletableFuture.failedFuture(primary), settings.snapshot(),
                event -> { if (event.type() == AtfConsoleEvent.Type.COMPLETE) throw publication; }, ApiTransportMode.ACCEPTANCE);
        var publishedFailure = assertThrows(ApiExecutionException.class, () -> reporter.execute(REQUEST));
        assertSame(primary, publishedFailure.getCause()); assertEquals(List.of(publication), List.of(publishedFailure.getSuppressed()));
        assertEquals(ApiTransportProvenance.ACCEPTANCE_MOCK, snapshot(publishedFailure).provenance());
    }

    @Test void aPolicyFailureAfterAResponseDoesNotClaimThatNoResponseWasCaptured() {
        var original = new IllegalStateException("controlled policy failure");
        RetryPolicy brokenPolicy = (response, attempt) -> { throw original; };
        var executor = new ApiExecutor((request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "actual response", Map.of(), Duration.ZERO, url)), ORIGIN, brokenPolicy, new Clock());
        var failure = assertThrows(ApiExecutionException.class, () -> executor.execute(REQUEST));
        assertSame(original, failure.getCause()); assertEquals(200, snapshot(failure).response().orElseThrow().status());
        assertEquals(200, snapshot(failure).attemptHistory().getFirst().httpStatus().orElseThrow());
        assertEquals(1, snapshot(failure).attempts()); assertEquals(ApiAttemptOutcome.FAILED, snapshot(failure).termination());
    }
    @Test void non2xxFinalResponsesAndLegacyExceptionsRemainCompatible() {
        var clock = new Clock(); var calls = new AtomicInteger();
        var result = executor((request, url, auth) -> { calls.incrementAndGet(); return CompletableFuture.completedFuture(busy(url)); },
                clock, Duration.ZERO).execute(REQUEST);
        assertEquals(503, result.response().status()); assertEquals(2, result.attempts()); assertEquals(2, calls.get());
        assertEquals(ApiAttemptOutcome.SUCCESS, result.attemptHistory().getLast().outcome());
        assertTrue(new ApiExecutionException("legacy", null).snapshot().isEmpty());
        var unrelated = new IllegalStateException("unrelated owner");
        unrelated.addSuppressed(assertThrows(ApiExecutionException.class,
                () -> executor((request, url, auth) -> CompletableFuture.failedFuture(new IOException()), clock, Duration.ZERO).execute(REQUEST)));
        assertTrue(ApiExecutionFailure.find(unrelated).isEmpty());
    }
}
