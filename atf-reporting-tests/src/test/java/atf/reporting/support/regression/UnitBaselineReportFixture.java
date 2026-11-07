package atf.reporting.support.regression;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.*;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Distinct phase/request/timing witnesses prevent a passing setup from impersonating a failed body. */
public final class UnitBaselineReportFixture {
    private UnitBaselineReportFixture() { }

    public static Path publish(Path report, boolean fallback) {
        var settings = new ReportingSettings(240);
        try (AtfReportPorts.Session session = fallback
                ? new HtmlReportSession(report, settings) : new ExtentReportSession(report, settings)) {
            var scenario = session.startScenario("BaselineAudit", "atf.fixture", "Independent request ownership", List.of("component", "synthetic"));
            var setup = scenario.startPhase("SETUP");
            request(setup, "GET /setup-decoy", "/setup-decoy", Duration.ofMillis(999), true);
            setup.complete(AtfReportStatus.PASS); setup.close();
            var body = scenario.startPhase("TEST BODY");
            request(body, "GET /audit-target", "/audit-target", Duration.ofMillis(12), false);
            body.complete(AtfReportStatus.FAIL); body.close();
            scenario.complete(AtfReportStatus.FAIL); scenario.close();
        }
        return report;
    }

    private static void request(AtfReportPorts.Phase phase, String label, String path, Duration duration, boolean passed) {
        var request = phase.startRequest(label, "GET", path);
        var observed = ReportRequest.builder("GET", path).build();
        var response = new ReportResponse(passed ? 201 : 200, "{\"id\":1}", Map.of("Content-Type", "application/json"),
                duration, "https://audit.invalid" + path);
        request.execution(new ReportExecutionEvidence(response, 1, duration, List.of(), List.of(), observed,
                ReportTransportProvenance.ACCEPTANCE_MOCK));
        var validation = request.startValidation("HTTP contract");
        validation.evidenceOwner(ReportEvidenceOwner.API);
        validation.assertions(List.of(new AtfAssertion("HTTP status", "201", passed ? "201" : "200", passed)));
        validation.complete(passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL); validation.close();
        request.complete(passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL); request.close();
    }
}
