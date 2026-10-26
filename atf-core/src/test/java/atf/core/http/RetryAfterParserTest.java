package atf.core.http;
import atf.testkit.AtfUnitTest;

import atf.core.http.response.ApiResponse;
import atf.core.http.transport.RetryPolicy;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest

class RetryAfterParserTest {
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void parsesDeltaSecondsWithoutUsingWallClock() {
        assertEquals(Duration.ofSeconds(3),
                RetryPolicy.retryAfter("3", Duration.ofSeconds(9), CLOCK));
        assertEquals(Duration.ZERO,
                RetryPolicy.retryAfter("-3", Duration.ofSeconds(9), CLOCK));
    }

    @Test
    void parsesIsoAndHttpDateAgainstInjectedClock() {
        assertEquals(Duration.ofSeconds(7), RetryPolicy.retryAfter(
                "2026-10-14T12:00:07Z", Duration.ofSeconds(9), CLOCK));
        assertEquals(Duration.ofSeconds(7), RetryPolicy.retryAfter(
                "Wed, 14 Oct 2026 12:00:07 GMT", Duration.ofSeconds(9), CLOCK));
    }

    @Test
    void clampsPastDatesAndFallsBackForInvalidValues() {
        assertEquals(Duration.ZERO, RetryPolicy.retryAfter(
                "2026-10-14T11:59:59Z", Duration.ofSeconds(9), CLOCK));
        assertEquals(Duration.ofSeconds(9), RetryPolicy.retryAfter(
                "not-a-date", Duration.ofSeconds(9), CLOCK));
    }

    @Test
    void statusPolicyUsesTheInjectedClockForAbsoluteRetryAfter() {
        var response = new ApiResponse(429, "busy",
                java.util.Map.of("Retry-After", "2026-10-14T12:00:07Z"),
                Duration.ZERO, "https://example.test/users");

        var event = RetryPolicy.onStatuses(java.util.Set.of(429), 2,
                Duration.ofSeconds(9), CLOCK).next(response, 1).orElseThrow();

        assertEquals(Duration.ofSeconds(7), event.delay());
    }
}
