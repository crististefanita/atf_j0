package atf.autoex.tests.component.api;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.*;
import atf.core.http.transport.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@AutoExComponentTest
class AutoExTerminalExecutionComponentTest {
    private static final String SECRET = "terminal-private-token-must-not-leak";
    @ParameterizedTest(name = "Terminal API evidence · {0}")
    @ValueSource(strings = {"EXTENT", "FALLBACK"})
    void a503AndTerminalFailureStayAuditableWithoutAnotherDispatch(String renderer, RecordedAssertions checks) throws Exception {
        var calls = new AtomicInteger();
        var cause = new IOException("controlled terminal transport failure; authorization: Bearer " + SECRET);
        ApiTransport transport = (request, url, auth) -> calls.incrementAndGet() == 1
                ? CompletableFuture.completedFuture(new ApiResponse(503, "busy", Map.of(), Duration.ofMillis(2), url))
                : CompletableFuture.failedFuture(cause);
        ApiRequest request = new atf.autoex.api.AutoExApiRequestFactory().productsList();
        var executor = new ApiExecutor(transport, URI.create("https://terminal.fixture.test"),
                RetryPolicy.onStatuses(Set.of(503), 2, Duration.ZERO));
        var failure = assertThrows(ApiExecutionException.class, () -> executor.execute(request), "Controlled terminal failure remains observable");
        var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
        checks.equals("Original transport dispatch count", 2, calls.get());
        checks.isTrue("Terminal cause identity is retained", failure.getCause() == cause);
        checks.equals("Actual attempts in partial snapshot", 2, snapshot.attempts());
        checks.equals("Initial HTTP status", 503, snapshot.attemptHistory().getFirst().httpStatus().orElseThrow());
        checks.isTrue("Terminal HTTP status is absent", snapshot.attemptHistory().getLast().httpStatus().isEmpty());
        checks.isTrue("Terminal response is explicitly absent", snapshot.response().isEmpty());

        Path directory = Files.createDirectories(Path.of("target", "terminal-api-evidence", UUID.randomUUID().toString()));
        Path report = directory.resolve(renderer.toLowerCase(Locale.ROOT) + ".html");
        AtfReportPorts.Session session = renderer.equals("FALLBACK") ? new HtmlReportSession(report) : new ExtentReportSession(report);
        try (session) {
            var scenario = session.startScenario("AutoExTerminalFailureFixture", getClass().getPackageName(),
                    "503 then terminal transport failure", List.of("component", "controlled-terminal-failure"));
            var phase = scenario.startPhase("TEST BODY");
            var diagnostic = atf.autoex.runtime.api.AutoExApiFailureEvidence.publish(phase, failure,
                    failure + " <- " + cause).orElseThrow();
            checks.isTrue("Published diagnostic stays within 240", diagnostic.length() <= 240);
            phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, diagnostic);
            phase.complete(AtfReportStatus.FAIL); scenario.complete(AtfReportStatus.FAIL);
        }
        String html = Files.readString(report);
        checks.equals("Publishing evidence does not replay transport", 2, calls.get());
        checks.isTrue("Earlier 503 remains in the report", html.contains("503"));
        checks.isTrue("Terminal attempt has unavailable HTTP status", html.contains("Unavailable"));
        checks.isTrue("Terminal diagnostic is inspectable", html.contains("controlled terminal transport failure"));
        checks.isTrue("Terminal outcome remains failed", html.contains("FAILED"));
        checks.isFalse("Full diagnostic is redacted before publication", html.contains(SECRET));
        checks.isTrue("Failed request retains execution evidence", html.contains("execution-evidence"));
    }
}
