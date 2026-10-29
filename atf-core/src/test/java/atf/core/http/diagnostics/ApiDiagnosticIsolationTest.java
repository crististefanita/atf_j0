package atf.core.http.diagnostics;

import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.CancellationToken;
import atf.core.config.ApiSettings;
import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.*;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ApiDiagnosticIsolationTest {
    @ParameterizedTest @EnumSource(value = AtfConsoleEvent.Type.class, names = {"START", "DETAIL", "CODE", "COMPLETE"})
    void aNonfatalSinkFailureCannotStopOrReplaceTheOneSuccessfulDispatch(AtfConsoleEvent.Type type) {
        var calls = new AtomicInteger(); var diagnosticCalls = new AtomicInteger();
        var expected = new ApiResponse(200, "{}", Map.of(), Duration.ofMillis(1), "https://fixture.test/one");
        var executor = new ApiExecutor((request, url, auth) -> { calls.incrementAndGet(); return CompletableFuture.completedFuture(expected); },
                URI.create("https://fixture.test"), event -> {
                    if (event.type() == type) { diagnosticCalls.incrementAndGet(); throw new IllegalStateException("diagnostic unavailable"); }
                });
        var result = executor.execute(ApiRequest.of("GET", "/one"));
        assertSame(expected, result.response()); assertEquals(1, calls.get());
        assertEquals(1, result.attemptHistory().size()); assertTrue(diagnosticCalls.get() > 0);
    }

    @Test void functionalFailureRetainsItsSnapshotAndTheDistinctDiagnosticCause() {
        var calls = new AtomicInteger();
        var primary = new IllegalStateException("controlled transport failure");
        var diagnostic = new IllegalStateException("controlled sink failure");
        var executor = new ApiExecutor((request, url, auth) -> { calls.incrementAndGet(); return CompletableFuture.failedFuture(primary); },
                URI.create("https://fixture.test"), event -> { throw diagnostic; });
        var failure = assertThrows(ApiExecutionException.class, () -> executor.execute(ApiRequest.of("GET", "/one")));
        assertSame(primary, failure.getCause()); assertEquals(List.of(diagnostic), Arrays.asList(failure.getSuppressed()));
        assertEquals(1, calls.get());
        var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
        assertTrue(snapshot.response().isEmpty()); assertEquals(1, snapshot.attemptHistory().size());
    }

    @Test void fatalVmOrLinkageErrorsAreNotSilentlyConvertedIntoSuccessfulDiagnostics() {
        var calls = new AtomicInteger(); var fatal = new LinkageError("controlled linkage failure");
        var executor = new ApiExecutor((request, url, auth) -> { calls.incrementAndGet(); return CompletableFuture.completedFuture(null); },
                URI.create("https://fixture.test"), event -> { throw fatal; });
        assertSame(fatal, assertThrows(LinkageError.class, () -> executor.execute(ApiRequest.of("GET", "/one"))));
        assertEquals(0, calls.get());
    }

    @Test void failingDiagnosticsPreserveAPlannedRetryAndItsTwoActualDispatches() {
        var calls = new AtomicInteger();
        var settings = new ApiSettings(URI.create("https://fixture.test"), "none", Duration.ofSeconds(1), Set.of(503), 2);
        var executor = new ApiExecutor((request, url, auth) -> CompletableFuture.completedFuture(new ApiResponse(
                calls.incrementAndGet() == 1 ? 503 : 200, "{}", Map.of(), Duration.ZERO, url)), settings.snapshot(),
                event -> { throw new IllegalStateException("controlled diagnostic failure"); });
        var result = executor.execute(ApiRequest.of("GET", "/one"));
        assertEquals(2, calls.get()); assertEquals(200, result.response().status()); assertEquals(1, result.retryEvents().size());
        assertEquals(List.of(ApiAttemptOutcome.RETRYABLE_RESPONSE, ApiAttemptOutcome.SUCCESS),
                result.attemptHistory().stream().map(ApiAttemptRecord::outcome).toList());
    }

    @Test void failingDiagnosticsCannotDispatchACancelledRequestOrEraseItsEmptySnapshot() {
        var calls = new AtomicInteger(); var diagnostic = new IllegalStateException("controlled diagnostic failure");
        var executor = new ApiExecutor((request, url, auth) -> { calls.incrementAndGet(); return new CompletableFuture<>(); },
                URI.create("https://fixture.test"), event -> { throw diagnostic; });
        var failure = assertThrows(CancellationException.class,
                () -> executor.execute(ApiRequest.of("GET", "/one"), Duration.ofSeconds(1), () -> true));
        var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
        assertEquals(0, calls.get()); assertEquals(0, snapshot.attempts()); assertEquals(ApiAttemptOutcome.CANCELLED, snapshot.termination());
        assertEquals(List.of(diagnostic), Arrays.asList(failure.getSuppressed()));
    }

    @Test void failingDiagnosticsDoNotExtendTheDeadlineOrLeaveTheTimedOutFutureRunning() {
        var pending = new CompletableFuture<ApiResponse>(); var diagnostic = new IllegalStateException("controlled diagnostic failure");
        var executor = new ApiExecutor((request, url, auth) -> pending, URI.create("https://fixture.test"), event -> { throw diagnostic; });
        var failure = assertThrows(ApiExecutionException.class,
                () -> executor.execute(ApiRequest.of("GET", "/one"), Duration.ofMillis(10), CancellationToken.none()));
        var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
        assertInstanceOf(TimeoutException.class, failure.getCause()); assertTrue(pending.isCancelled());
        assertEquals(1, snapshot.attempts()); assertEquals(ApiAttemptOutcome.TIMED_OUT, snapshot.termination());
        assertEquals(List.of(diagnostic), Arrays.asList(failure.getSuppressed()));
    }
}
