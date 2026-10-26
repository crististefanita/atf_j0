package atf.reporting.diagnostic;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.JUNIT_FAILURE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Public diagnostic publication must preserve verdict, redact and never fabricate HTTP exchanges. */
@AtfComponentTest
class ReportPhaseDiagnosticPublicationComponentTest {
    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource({"true,PASS", "false,PASS", "true,FAIL", "false,FAIL"})
    void diagnosticsAreRedactedWithoutInventingAnExchangeOrRewritingTheVerdict(
            boolean extent, AtfReportStatus status) throws Exception {
        Path report = directory.resolve("diagnostic.html");
        String secret = "projection-diagnostic-secret";
        try (var session = extent ? new ExtentReportSession(report) : new HtmlReportSession(report)) {
            var scenario = session.startScenario("DiagnosticContract", "phase diagnostic", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            phase.diagnostic(JUNIT_FAILURE, "operation failed; authorization: Bearer " + secret);
            phase.complete(status);
            scenario.complete(status);
            var snapshot = session instanceof ExtentReportSession renderer
                    ? renderer.semanticSnapshot() : ((HtmlReportSession) session).semanticSnapshot();
            assertEquals(0, snapshot.requestCount());
            assertEquals(0, snapshot.validationCount());
            var observed = snapshot.classes().getFirst().scenarios().getFirst();
            assertEquals(status, observed.status());
            assertEquals(List.of("JUnit failure"), observed.phases().getFirst().detailNames());
        }
        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Execution diagnostics")),
                () -> assertTrue(html.contains("operation failed")),
                () -> assertFalse(html.contains(secret)));
    }
}
