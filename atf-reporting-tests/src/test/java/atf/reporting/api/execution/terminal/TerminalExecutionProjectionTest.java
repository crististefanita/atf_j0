package atf.reporting.api.execution.terminal;

import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionException;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.RetryPolicy;
import atf.reporting.api.*;
import atf.reporting.api.evidence.ReportExecutionSummary;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class TerminalExecutionProjectionTest {
    @Test void partialAdapterRetainsTheActualRequestAndNormalizesNoResponseAvailability() {
        var calls = new AtomicInteger();
        var request = ApiRequest.of("GET", "/read");
        var executor = new ApiExecutor((sent, url, auth) -> calls.incrementAndGet() == 1
                ? CompletableFuture.completedFuture(new ApiResponse(503, "busy", Map.of(), Duration.ZERO, url))
                : CompletableFuture.failedFuture(new java.io.IOException("controlled terminal failure")),
                URI.create("https://fixture.test"), RetryPolicy.onStatuses(Set.of(503), 2, Duration.ZERO));
        var failure = assertThrows(ApiExecutionException.class, () -> executor.execute(request));
        var adapted = ReportModelAdapters.partialExecution(ApiExecutionFailure.find(failure).orElseThrow());
        assertEquals(2, calls.get()); assertEquals(2, adapted.attempts()); assertNull(adapted.response());
        assertEquals(request.method(), adapted.request().method()); assertEquals(request.path(), adapted.request().path());
        assertEquals(List.of("503", "Unavailable"), adapted.attemptHistory().stream().map(ReportAttemptRecord::statusLabel).toList());
        assertEquals("Unavailable", new ReportAttemptRecord(1, 0, Duration.ZERO, "RETRYABLE_FAILURE", "").statusLabel());
        assertThrows(IllegalArgumentException.class, () -> ReportModelAdapters.partialExecution(
                new atf.core.http.response.ApiExecutionResult(new ApiResponse(200, "", Map.of(), Duration.ZERO, ""), 1, Duration.ZERO, List.of())));
    }

    @Test void terminationIsTypedImmutableAndUsesTheConfiguredDiagnosticBudget() {
        var raw = new ReportExecutionEvidence(null, 0, Duration.ZERO, List.of(), List.of(),
                ReportRequest.builder("GET", "/read").build(), ReportTransportProvenance.UNKNOWN);
        String secret = "projection-private-token";
        var terminal = raw.withTermination("CANCELLED", "safe-prefix ".repeat(40) + "; authorization: Bearer " + secret);
        assertTrue(raw.termination().isEmpty()); assertEquals("CANCELLED", terminal.termination().orElseThrow().outcome());
        var table = ReportExecutionSummary.table(terminal, new SensitiveDataRedactor(), new ReportingSettings(18));
        var diagnostic = table.rows().stream().filter(row -> row.getFirst().equalsIgnoreCase("Execution failure")).findFirst().orElseThrow().getLast();
        assertEquals(18, diagnostic.length()); assertTrue(diagnostic.endsWith("…")); assertFalse(diagnostic.contains(secret));
        assertFalse(table.rows().stream().anyMatch(row -> row.getFirst().equalsIgnoreCase("Response duration")));
        assertTrue(table.rows().stream().anyMatch(row -> row.getLast().contains("No HTTP response received")));
    }
}
