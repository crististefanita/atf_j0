package atf.ui.tests.extent.support.inspection.table;

import atf.reporting.api.*;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.network.ReportNetworkInspector;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

import static atf.reporting.contract.AtfReportStatus.*;

/** Controlled component evidence; no store, browser traffic or consumer business classes. */
public final class PublishedEvidenceTableFixture {
    private PublishedEvidenceTableFixture() { }

    public enum Profile { UI, API, UI_API }
    public enum Shape { SHORT, LONG_EXPECTED, LONG_ACTUAL }

    public static Path publish(Profile profile, boolean fallback) {
        Path path = TestReportPaths.report("published-tables-" + profile.name().toLowerCase(Locale.ROOT)
                + (fallback ? "-fallback-" : "-extent-") + UUID.randomUUID() + ".html");
        Map<String, String> environment = Map.of("Profile", profile.name(), "Fixture", "Published table component",
                "Source", "Controlled in-memory evidence; no live HTTP", "Unicode", "Ω <edge>");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(path)
                : new ExtentReportSession(path, ReportingSettings.defaults(), new SensitiveDataRedactor(),
                AtfConsoleSink.noOp(), environment)) {
            for (Shape shape : Shape.values()) {
                var scenario = session.startScenario("PublishedTableComponent", "atf.reporting.component",
                        shape.name(), List.of("component", profile.name().toLowerCase(Locale.ROOT)));
                var setup = scenario.startPhase("SETUP");
                var configuration = setup.startRequest("GIVEN typed table evidence is ready", "", "");
                configuration.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.REPORT_SETUP);
                for (int i = 1; i <= 3; i++) configuration.detail(AtfReportEvidenceKeys.setupCheck(i),
                        "Configuration " + i + " | expected=ready | actual=ready | result=PASS");
                configuration.complete(PASS);
                setup.complete(PASS);
                var body = scenario.startPhase("TEST BODY");
                List<AtfAssertion> assertions = assertions(shape);
                AtfReportStatus status = shape == Shape.SHORT ? PASS : FAIL;
                var component = body.startAssertionStep("Component assertions");
                component.assertions(assertions);
                component.complete(status);
                if (profile != Profile.API) {
                    var screen = body.startRequest("THEN the screen evidence is inspectable", "", "");
                    var validation = screen.startValidation("UI validation");
                    validation.uiActions(actions());
                    validation.screenshot("Controlled fixture image", TableViewportReportFixture.PNG);
                    validation.assertions(assertions);
                    if (profile == Profile.UI_API) api(validation.startRequest(
                            "GET /table-evidence — correlated exchange", "GET", "/table-evidence"), shape, assertions);
                    validation.complete(status);
                    screen.complete(status);
                } else {
                    api(body.startRequest("GET /table-evidence", "GET", "/table-evidence"), shape, assertions);
                }
                body.complete(status);
                scenario.complete(status);
            }
        }
        return path;
    }

    private static void api(AtfReportPorts.Request scope, Shape shape, List<AtfAssertion> assertions) {
        int count = shape == Shape.SHORT ? 1 : 40;
        for (int i = 1; i <= count; i++) {
            scope.detail("request.header.X-Table-" + i, "request value " + i);
            scope.detail("response.detail." + i, "response value " + i);
        }
        var request = new ReportRequest("GET", "/table-evidence", Map.of("query", "Ω <edge>"),
                Map.of("Accept", "application/json"), "none", "");
        var response = new ReportResponse(200, "{\"message\":\"Ω <edge>\",\"count\":9}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(3),
                "https://api.example.test/table-evidence");
        var history = IntStream.rangeClosed(1, count).mapToObj(i -> new ReportAttemptRecord(i, 200,
                Duration.ofMillis(3), "SUCCESS", "independent attempt " + i)).toList();
        var trace = ReportTraceMetadata.from(request).withCapture("table-exchange", 1, "TEST BODY",
                "ActionCaused", "table-checkpoint", "component fixture", "ACCEPTANCE_MOCK", "",
                "complete request and response evidence", "observed Ω <edge>");
        var execution = new ReportExecutionEvidence(response, count, Duration.ofMillis(count * 3L),
                List.of(), history, request, ReportTransportProvenance.ACCEPTANCE_MOCK, trace);
        scope.execution(execution);
        scope.networkAggregate(ReportNetworkInspector.aggregate(List.of(execution), List.of()));
        scope.json("RESPONSE JSON", response.body());
        var validation = scope.startValidation("API assertions");
        validation.assertions(assertions);
        AtfReportStatus status = shape == Shape.SHORT ? PASS : FAIL;
        validation.complete(status);
        scope.complete(status);
    }

    private static List<AtfUiActionRecord> actions() {
        var actions = new ArrayList<AtfUiActionRecord>();
        for (int i = 1; i <= 5; i++) actions.add(new AtfUiActionRecord(i, "Get text",
                "#component-" + i, "visible Ω <edge>", "visible Ω <edge>", i, PASS));
        for (int i = 6; i <= 7; i++) actions.add(new AtfUiActionRecord(i, "Audit observation",
                "checkpoint-" + i, "independent state", "observed state", 0, PASS, AtfUiEvidenceSource.OBSERVE));
        return List.copyOf(actions);
    }

    private static List<AtfAssertion> assertions(Shape shape) {
        if (shape != Shape.SHORT) return IntStream.rangeClosed(1, 40).mapToObj(i -> new AtfAssertion(
                "Independent comparison " + i,
                shape == Shape.LONG_EXPECTED ? "Expected Ω <edge> ".repeat(7) + "W".repeat(60) : "ready",
                shape == Shape.LONG_ACTUAL ? "Observed Ω <edge> ".repeat(7) + "W".repeat(60) : "different", false)).toList();
        String[][] rows = {
                {"Page route", "/", "/"}, {"catalog product cards", "visible", "visible"},
                {"catalog product names", "visible", "visible"}, {"catalog product images", "visible", "visible"},
                {"catalog pagination", "visible", "visible"}, {"catalog footer", "visible", "visible"},
                {"Product cards", "> 0", "9"}, {"Product card names", "9", "9"}, {"Product card images", "9", "9"},
                {"catalog pagination count", ">= 1", "1"},
                {"Style · catalog product cards · display", "flex", "flex"},
                {"Style · catalog footer · display", "block", "block"}};
        return Arrays.stream(rows).map(row -> new AtfAssertion(row[0], row[1], row[2], true)).toList();
    }
}

