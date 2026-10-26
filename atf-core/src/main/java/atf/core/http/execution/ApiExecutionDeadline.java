package atf.core.http.execution;

import java.time.Duration;
import java.util.Objects;

/** Monotonic deadline for one request execution and all of its retries. */
final class ApiExecutionDeadline {
    private final ApiExecutionClock clock;
    private final long startedAt;
    private final long deadline;

    ApiExecutionDeadline(ApiExecutionClock clock, Duration timeout) {
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.startedAt = clock.nanoTime();
        this.deadline = saturatingAdd(startedAt, timeout.toNanos());
    }

    long startedAt() {
        return startedAt;
    }

    long remainingNanos() {
        return deadline - clock.nanoTime();
    }

    long elapsedNanos() {
        return Math.max(0, clock.nanoTime() - startedAt);
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
