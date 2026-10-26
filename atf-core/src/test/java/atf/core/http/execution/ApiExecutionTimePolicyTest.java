package atf.core.http.execution;
import atf.testkit.AtfUnitTest;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiExecutionException;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.RetryPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

@AtfUnitTest

class ApiExecutionTimePolicyTest {
    @Test
    void deadlineUsesMonotonicClockAndReportsElapsedTime() {
        var clock = new FakeClock();
        var deadline = new ApiExecutionDeadline(clock, Duration.ofNanos(100));

        clock.advance(40);
        assertEquals(60, deadline.remainingNanos());
        clock.advance(100);
        assertEquals(-40, deadline.remainingNanos());
        assertEquals(140, deadline.elapsedNanos());
    }

    @Test
    void retryWaiterConsumesDelayWithoutWallClockSleep() {
        var clock = new FakeClock();
        var deadline = new ApiExecutionDeadline(clock, Duration.ofMillis(200));
        var waiter = new ApiRetryWaiter(clock, deadline);

        waiter.await(Duration.ofMillis(125), CancellationToken.none());

        assertEquals(125_000_000L, clock.now.get());
    }

    @Test
    void retryWaiterStopsWhenDelayWouldOutliveExecutionDeadline() {
        var clock = new FakeClock();
        var deadline = new ApiExecutionDeadline(clock, Duration.ofMillis(20));
        var waiter = new ApiRetryWaiter(clock, deadline);

        assertThrows(ApiExecutionException.class,
                () -> waiter.await(Duration.ofMillis(21), CancellationToken.none()));
    }

    @Test
    void executorUsesTheInjectedClockForRetryHistoryAndTotalDuration() {
        var clock = new FakeClock();
        var calls = new AtomicInteger();
        ApiTransport transport = (request, url, auth) -> {
            int call = calls.incrementAndGet();
            int status = call == 1 ? 429 : 200;
            return CompletableFuture.completedFuture(
                    new ApiResponse(status, "{}", Map.of(), Duration.ZERO, url));
        };
        var executor = new ApiExecutor(transport, URI.create("https://example.test"),
                RetryPolicy.onStatuses(Set.of(429), 2, Duration.ofMillis(10)), clock);

        var result = executor.execute(ApiRequest.of("GET", "/users"),
                Duration.ofMillis(100), CancellationToken.none());

        assertEquals(2, result.attempts());
        assertEquals(2, result.attemptHistory().size());
        assertEquals(Duration.ofMillis(10), result.totalDuration());
        assertEquals(ApiAttemptOutcome.RETRYABLE_RESPONSE, result.attemptHistory().getFirst().outcome());
        assertEquals(ApiAttemptOutcome.SUCCESS, result.attemptHistory().getLast().outcome());
    }

    private static final class FakeClock implements ApiExecutionClock {
        private final AtomicLong now = new AtomicLong();

        @Override
        public long nanoTime() {
            return now.get();
        }

        @Override
        public void sleepNanos(long nanos) {
            now.addAndGet(nanos);
        }

        void advance(long nanos) {
            now.addAndGet(nanos);
        }
    }
}
