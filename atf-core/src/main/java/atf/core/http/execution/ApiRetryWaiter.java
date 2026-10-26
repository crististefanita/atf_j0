package atf.core.http.execution;
import atf.core.http.response.ApiExecutionException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/** Applies retry delays while preserving the deadline and cancellation policy. */
final class ApiRetryWaiter {
    private final ApiExecutionClock clock;
    private final ApiExecutionDeadline deadline;

    ApiRetryWaiter(ApiExecutionClock clock, ApiExecutionDeadline deadline) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
    }

    void await(Duration delay, CancellationToken cancellationToken) {
        Objects.requireNonNull(delay, "delay");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        long remaining = Math.max(0, delay.toNanos());
        while (remaining > 0) {
            cancellationToken.throwIfCancelled();
            long budget = deadline.remainingNanos();
            if (budget <= 0) {
                throw new ApiExecutionException("Retry delay exceeded execution timeout", new TimeoutException());
            }
            long slice = Math.min(remaining, Math.min(budget, 50_000_000L));
            try {
                clock.sleepNanos(slice);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new ApiExecutionException("Retry interrupted", error);
            }
            remaining -= slice;
        }
    }
}
