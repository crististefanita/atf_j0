package atf.core.http.execution;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/** Parses the HTTP Retry-After forms without owning retry decisions. */
public final class RetryAfterParser {
    private RetryAfterParser() { }

    public static Duration parse(String value, Duration fallback, Clock clock) {
        Objects.requireNonNull(fallback, "fallback");
        Objects.requireNonNull(clock, "clock");
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String candidate = value.trim();
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(candidate)));
        } catch (NumberFormatException ignored) {
            // Retry-After also permits an absolute HTTP date. Continue with
            // the two supported date representations.
        }

        Instant now = clock.instant();
        try {
            return nonNegativeDuration(now, Instant.parse(candidate));
        } catch (DateTimeParseException ignoredIso) {
            try {
                return nonNegativeDuration(now,
                        ZonedDateTime.parse(candidate, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
            } catch (DateTimeParseException ignoredHttpDate) {
                return fallback;
            }
        }
    }

    private static Duration nonNegativeDuration(Instant now, Instant target) {
        Duration delay = Duration.between(now, target);
        return delay.isNegative() ? Duration.ZERO : delay;
    }
}
