package atf.core;
import atf.testkit.AtfUnitTest;

import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.CancellationToken;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiExecutionException;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.RetryPolicy;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class ExecutionResilienceTest {
    @Test
    void cancellationIsObservedBeforeTransportIsCalled() {
        AtomicInteger calls = new AtomicInteger();
        ApiTransport transport = (request, url, auth) -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(new ApiResponse(200, "{}", Map.of(), Duration.ZERO, url));
        };

        var executor = new ApiExecutor(transport, URI.create("https://example.test"));

        assertThrows(CancellationException.class,
                () -> executor.execute(ApiRequest.of("GET", "/users"), Duration.ofSeconds(1), () -> true));
        assertEquals(0, calls.get());
    }

    @Test
    void timeoutDoesNotWaitForeverForAnUncompletedTransport() {
        var pending = new CompletableFuture<ApiResponse>();
        ApiTransport transport = (request, url, auth) -> pending;
        var executor = new ApiExecutor(transport, URI.create("https://example.test"));

        var error = assertThrows(ApiExecutionException.class,
                () -> executor.execute(ApiRequest.of("GET", "/users"), Duration.ofMillis(30), CancellationToken.none()));

        assertTrue(error.getMessage().contains("timed out"));
        assertTrue(pending.isCancelled());
    }

    @Test
    void cancellationDuringTransportCancelsThePendingFuture() throws Exception {
        var pending = new CompletableFuture<ApiResponse>();
        ApiTransport transport = (request, url, auth) -> pending;
        var executor = new ApiExecutor(transport, URI.create("https://example.test"));
        var cancelled = new AtomicBoolean();
        var worker = new Thread(() -> assertThrows(CancellationException.class,
                () -> executor.execute(ApiRequest.of("GET", "/users"), Duration.ofSeconds(2),
                        () -> cancelled.get())));
        worker.start();
        Thread.sleep(80);
        cancelled.set(true);
        worker.join(1000);

        assertFalse(worker.isAlive());
        assertTrue(pending.isCancelled());
    }

    @Test
    void transportFailureCanBeRetriedAndTheHistoryKeepsTheAttempt() {
        AtomicInteger calls = new AtomicInteger();
        ApiTransport transport = (request, url, auth) -> {
            if (calls.incrementAndGet() == 1) {
                var failed = new CompletableFuture<ApiResponse>();
                failed.completeExceptionally(new IllegalStateException("temporary"));
                return failed;
            }
            return CompletableFuture.completedFuture(new ApiResponse(200, "{}", Map.of(), Duration.ZERO, url));
        };
        var executor = new ApiExecutor(transport, URI.create("https://example.test"),
                RetryPolicy.onStatusesAndFailures(Set.of(503), 2, Duration.ZERO));

        var result = executor.execute(ApiRequest.of("GET", "/users"));

        assertEquals(2, calls.get());
        assertEquals(2, result.attempts());
        assertEquals("transient transport failure", result.retryEvents().getFirst().reason());
        assertEquals(0, result.retryEvents().getFirst().statusCode());
        assertEquals(2, result.attemptHistory().size());
        assertEquals(ApiAttemptOutcome.RETRYABLE_FAILURE, result.attemptHistory().getFirst().outcome());
        assertEquals(ApiAttemptOutcome.SUCCESS, result.attemptHistory().getLast().outcome());
        assertEquals(200, result.attemptHistory().getLast().statusCode());
    }

    @Test
    void successfulExecutionPublishesOneDeterministicAttemptRecord() {
        ApiTransport transport = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(204, "", Map.of(), Duration.ZERO, url));

        var result = new ApiExecutor(transport, URI.create("https://example.test"))
                .execute(ApiRequest.of("GET", "/health"));

        assertEquals(1, result.attemptHistory().size());
        var record = result.attemptHistory().getFirst();
        assertEquals(1, record.number());
        assertEquals(204, record.statusCode());
        assertEquals(ApiAttemptOutcome.SUCCESS, record.outcome());
        assertFalse(record.duration().isNegative());
    }
}
