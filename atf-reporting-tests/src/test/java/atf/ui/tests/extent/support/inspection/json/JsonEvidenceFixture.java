package atf.ui.tests.extent.support.inspection.json;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportJsonBody;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Authored mock evidence, independent from application flows and production JSON serializers. */
public final class JsonEvidenceFixture {
    public static final String SUBJECT = "JSON fidelity subject";
    public static final String RAW = "{\"values\":[9007199254740993.0,0.12345678901234567890123456789,"
            + "1e309,1e-309,-0.0,1.2300],\"seen\":1,\"password\":\"synthetic-json-secret-a\","
            + "\"seen\":2,\"password\":[\"synthetic-json-secret-b\"]}";
    public static final List<String> NUMBERS = List.of("9007199254740993.0", "0.12345678901234567890123456789",
            "1e309", "1e-309", "-0.0", "1.2300", "1", "2");
    public static final String EXPECTED = "{\"amount\":9007199254740992.0}";
    public static final String ACTUAL = "{\"amount\":9007199254740993.0}";
    private static final String NUMERIC_BODY = "{\"values\":[9007199254740993.0,0.12345678901234567890123456789,"
            + "1e309,1e-309,-0.0,1.2300]}";
    private static final String DUPLICATE_BODY = "{\"seen\":1,\"password\":\"synthetic-json-secret-a\","
            + "\"seen\":2,\"password\":[\"synthetic-json-secret-b\"]}";

    private JsonEvidenceFixture() { }

    public static Path publish(boolean fallback, boolean aggregate) throws Exception {
        Path report = Files.createTempDirectory("atf-json-fidelity-")
                .resolve((fallback ? "fallback" : "extent") + "-" + aggregate + ".html");
        var settings = new ReportingSettings(240, 240);
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(report, settings) : new ExtentReportSession(report, settings)) {
            if (aggregate) scenario(session, "JSON neighbor before", true);
            scenario(session, SUBJECT, false);
            if (aggregate) scenario(session, "JSON neighbor after", true);
        }
        return report;
    }

    public static List<AtfAssertion> comparisons() {
        String prefix = "{\"value\":\"" + "W".repeat(260);
        return List.of(new AtfAssertion("Short distinct JSON", EXPECTED, ACTUAL, false),
                new AtfAssertion("Difference after preview", prefix + "expected\"}", prefix + "actual\"}", false),
                new AtfAssertion("JSON null · final comparison", "null", "null", true));
    }

    private static void scenario(AtfReportPorts.Session session, String title, boolean neighbor) {
        var status = neighbor ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        var scenario = session.startScenario("JsonFidelityMock", title, List.of("component", "api", "mock"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("GET /json — " + title, "GET", "/json");
        var duration = Duration.ofMillis(17);
        request.execution(new ReportExecutionEvidence(new ReportResponse(200, RAW,
                Map.of("Content-Type", "application/json"), duration, "https://mock.example.test/json"),
                1, duration, List.of(), List.of(),
                new ReportRequest("GET", "/json", Map.of(), Map.of(), "none", ""),
                ReportTransportProvenance.ACCEPTANCE_MOCK));
        request.body(new ReportJsonBody("Response JSON · numbers", NUMERIC_BODY,
                ReportJsonBody.Direction.RESPONSE, ReportJsonBody.Format.JSON));
        request.body(new ReportJsonBody("Response JSON · duplicates", DUPLICATE_BODY,
                ReportJsonBody.Direction.RESPONSE, ReportJsonBody.Format.JSON));
        var validation = request.startValidation("Response contract");
        validation.evidenceOwner(ReportEvidenceOwner.API);
        validation.assertions(neighbor ? List.of(new AtfAssertion("Neighbor only", "neighbor", "neighbor", true)) : comparisons());
        validation.complete(status);
        request.complete(status);
        phase.complete(status);
        scenario.complete(status);
    }
}
