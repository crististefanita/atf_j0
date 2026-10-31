package atf.ui.tests.extent.support.inspection.disclosure;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Controlled mock snapshots through the public ports; no Eventuate or remote service. */
public final class ApiValidationDisclosureFixture {
    public static final String SUBJECT = "API disclosure subject";
    public static final String CANARY = "synthetic-disclosure-secret";
    private ApiValidationDisclosureFixture() { }

    public static Path publish(boolean fallback, boolean aggregate, AtfReportStatus status) throws Exception {
        return publish(fallback, aggregate, status, comparisons(status), "application/json");
    }

    public static Path publishMedia(boolean fallback, AtfReportStatus status) throws Exception {
        return publish(fallback, true, status, List.of(
                new AtfAssertion("HTTP status", "200", "200", true),
                new AtfAssertion("Content-Type JSON", "application/json", "text/plain; profile=\"application/json\"", false, status),
                new AtfAssertion("JSON null · final comparison", "null", "null", true)),
                "text/plain; profile=\"application/json\"");
    }

    private static Path publish(boolean fallback, boolean aggregate, AtfReportStatus status,
                                List<AtfAssertion> comparisons, String mediaType) throws Exception {
        Path report = Files.createTempDirectory("atf-api-validation-disclosure-")
                .resolve((fallback ? "fallback" : "extent") + "-" + aggregate + "-" + status + ".html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(report) : new ExtentReportSession(report)) {
            if (aggregate) scenario(session, "Neighbor before", AtfReportStatus.PASS, true, comparisons, mediaType);
            scenario(session, SUBJECT, status, false, comparisons, mediaType);
            if (aggregate) scenario(session, "Neighbor after", AtfReportStatus.PASS, true, comparisons, mediaType);
        }
        return report;
    }

    private static void scenario(AtfReportPorts.Session session, String title, AtfReportStatus status, boolean neighbor,
                                 List<AtfAssertion> comparisons, String mediaType) {
        var scenario = session.startScenario("ApiDisclosureMock", title, List.of("component", "api", "mock"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("GET /disclosure — " + title, "GET", "/disclosure");
        request.execution(exchange(mediaType));
        var validation = request.startValidation("Response contract");
        validation.evidenceOwner(ReportEvidenceOwner.API);
        validation.assertions(neighbor
                ? List.of(new AtfAssertion(title + " comparison", "neighbor-only", "neighbor-only", true))
                : comparisons);
        validation.complete(status);
        request.complete(status);
        phase.complete(status);
        scenario.complete(status);
    }

    public static List<AtfAssertion> comparisons(AtfReportStatus status) {
        boolean passed = status == AtfReportStatus.PASS;
        String prefix = "{\"value\":\"" + "W".repeat(260);
        return List.of(
                new AtfAssertion("JSON object", prefix + "expected\"}", prefix + (passed ? "expected" : "actual") + "\"}", passed, status),
                new AtfAssertion("JSON array", "[1,{\"password\":\"" + CANARY + "\"}]",
                        "[" + (passed ? "1" : "2") + ",{\"password\":\"" + CANARY + "\"}]", passed, status),
                new AtfAssertion("JSON null · final comparison", "null", passed ? "null" : "{}", passed, status));
    }

    private static ReportExecutionEvidence exchange(String mediaType) {
        var duration = Duration.ofMillis(17);
        return new ReportExecutionEvidence(
                new ReportResponse(200, "{\"values\":[{},[],null]}", Map.of("Content-Type", mediaType),
                        duration, "https://mock.example.test/disclosure"),
                1, duration, List.of(), List.of(),
                new ReportRequest("GET", "/disclosure", Map.of(), Map.of(), "none", ""),
                ReportTransportProvenance.ACCEPTANCE_MOCK);
    }
}
