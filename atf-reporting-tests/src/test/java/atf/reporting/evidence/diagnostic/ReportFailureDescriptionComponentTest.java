package atf.reporting.evidence.diagnostic;

import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Renderer integration is neutral; application adapters keep their own wiring probes. */
@AtfComponentTest
class ReportFailureDescriptionComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"extent", "fallback"})
    void bothRenderersPublishTheSafeFailureInItsOriginalPhase(String renderer) throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "failure-description", UUID.randomUUID().toString()));
        Path output = directory.resolve(renderer + ".html");
        String secret = "diagnostic-renderer-private-canary";
        var failure = new IllegalStateException("controlled execution failure", new java.io.IOException("password=" + secret));
        failure.addSuppressed(new IllegalArgumentException("controlled cleanup failure"));
        String diagnostic = ReportFailureDescription.describe(failure);
        AtfReportPorts.Session session = renderer.equals("extent") ? new ExtentReportSession(output) : new HtmlReportSession(output);
        try (session) {
            var scenario = session.startScenario("FailureDescription", "neutral failure graph", List.of("component"));
            try (scenario; var phase = scenario.startPhase("TEST BODY")) {
                phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, diagnostic);
                phase.complete(AtfReportStatus.FAIL);
                scenario.complete(AtfReportStatus.FAIL);
            }
        }
        String html = Files.readString(output);
        assertTrue(html.contains("controlled execution failure"));
        assertTrue(html.contains("controlled cleanup failure"));
        assertTrue(html.contains("TEST BODY") && html.contains("Failure cause"));
        var snapshot = session instanceof HtmlReportSession fallback ? fallback.semanticSnapshot()
                : ((ExtentReportSession) session).semanticSnapshot();
        assertEquals(AtfReportStatus.FAIL, snapshot.classes().getFirst().scenarios().getFirst().status());
        assertFalse(html.contains(secret));
        assertTrue(diagnostic.length() <= 240);
    }
}
