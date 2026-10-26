package atf.core.http.transport;
import atf.core.http.execution.RetryAfterParser;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.RetryEvent;

import java.time.Duration;
import java.time.Clock;
import java.util.Optional;

@FunctionalInterface
public interface RetryPolicy {
    Optional<RetryEvent> next(ApiResponse response, int attempt);

    /** Allows retries for transient transport failures without hiding the cause. */
    default Optional<RetryEvent> nextFailure(Throwable error, int attempt) {
        return Optional.empty();
    }

    static RetryPolicy none() {
        return (response, attempt) -> Optional.empty();
    }

    static RetryPolicy onStatuses(java.util.Set<Integer> statuses, int maxAttempts, Duration delay) {
        return onStatuses(statuses, maxAttempts, delay, Clock.systemUTC());
    }

    static RetryPolicy onStatuses(java.util.Set<Integer> statuses, int maxAttempts,
                                  Duration delay, Clock clock) {
        return (response, attempt) -> statuses.contains(response.status()) && attempt < maxAttempts
                ? Optional.of(new RetryEvent(attempt + 1, response.status(), response.header("Retry-After"),
                retryAfter(response.header("Retry-After"), delay, clock), "configured status retry"))
                : Optional.empty();
    }

    static RetryPolicy onStatusesAndFailures(java.util.Set<Integer> statuses, int maxAttempts, Duration delay) {
        return onStatusesAndFailures(statuses, maxAttempts, delay, Clock.systemUTC());
    }

    static RetryPolicy onStatusesAndFailures(java.util.Set<Integer> statuses, int maxAttempts,
                                             Duration delay, Clock clock) {
        return new RetryPolicy() {
            @Override
            public Optional<RetryEvent> next(ApiResponse response, int attempt) {
                return statuses.contains(response.status()) && attempt < maxAttempts
                        ? Optional.of(new RetryEvent(attempt + 1, response.status(), response.header("Retry-After"),
                        retryAfter(response.header("Retry-After"), delay, clock), "configured status retry"))
                        : Optional.empty();
            }

            @Override
            public Optional<RetryEvent> nextFailure(Throwable error, int attempt) {
                return attempt < maxAttempts
                        ? Optional.of(new RetryEvent(attempt + 1, 0, null, delay, "transient transport failure"))
                        : Optional.empty();
            }
        };
    }

    static Duration retryAfter(String value, Duration fallback) {
        return retryAfter(value, fallback, Clock.systemUTC());
    }

    static Duration retryAfter(String value, Duration fallback, Clock clock) {
        return RetryAfterParser.parse(value, fallback, clock);
    }
}
