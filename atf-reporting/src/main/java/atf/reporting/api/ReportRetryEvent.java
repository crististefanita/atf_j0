package atf.reporting.api;
import java.time.Duration;
public record ReportRetryEvent(int attempt, int statusCode, String header, Duration delay, String reason) { }
