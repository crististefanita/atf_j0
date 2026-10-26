package atf.core.http.response;

import java.time.Duration;

public record RetryEvent(int attempt, int statusCode, String header, Duration delay, String reason) { }
