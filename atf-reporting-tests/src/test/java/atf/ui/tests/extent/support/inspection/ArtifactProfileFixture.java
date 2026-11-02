package atf.ui.tests.extent.support.inspection;

import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Authored component/HTTP snapshots; Reporting does not perform a transport or query. */
public final class ArtifactProfileFixture {
    private ArtifactProfileFixture() { }

    public static ReportArtifactProfile publish(Path file, boolean fallback, boolean aggregate, boolean http) {
        var intended = new ArrayList<ReportArtifactProfile.Scenario>();
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file, ReportingSettings.defaults())) {
            for (int index : aggregate ? List.of(0, 1, 2) : List.of(1)) {
                boolean failed = index == 1;
                var status = failed ? AtfReportStatus.FAIL : AtfReportStatus.PASS;
                var comparisons = List.of(new ReportArtifactProfile.Comparison("numeric total", !failed),
                        new ReportArtifactProfile.Comparison("mismatch beyond preview", !failed),
                        new ReportArtifactProfile.Comparison("final original comparison", true));
                ReportArtifactProfile.Evidence node = http
                        ? new ReportArtifactProfile.HttpExchange("GET /fixture/orders", true, true, List.of("REQUEST JSON", "RESPONSE JSON"),
                                List.of(new ReportArtifactProfile.Assertions("Validate original response", comparisons)), status)
                        : new ReportArtifactProfile.Assertions("Original comparisons", comparisons);
                var expected = new ArrayList<ReportArtifactProfile.Evidence>();
                if (!http) expected.add(new ReportArtifactProfile.Observation("Original query", List.of("query id", "affected rows"), List.of("SQL text")));
                expected.add(node);
                intended.add(new ReportArtifactProfile.Scenario("ProfileFixture", "original case " + index, status,
                        List.of(new ReportArtifactProfile.Phase("TEST BODY", status, expected))));
                try (var scenario = session.startScenario("ProfileFixture", "original case " + index, List.of("component", "mock"));
                     var phase = scenario.startPhase("TEST BODY")) {
                    String prefix = "a".repeat(241);
                    var rows = List.of(new AtfAssertion("numeric total", "12", failed ? "13" : "12", !failed),
                            new AtfAssertion("mismatch beyond preview", prefix + "X", prefix + (failed ? "Y" : "X"), !failed),
                            new AtfAssertion("final original comparison", "ready", "ready", true));
                    if (http) {
                        try (var request = phase.startRequest("GET /fixture/orders", "GET", "/fixture/orders")) {
                            request.json("REQUEST JSON", "{\"query\":1}"); request.json("RESPONSE JSON", "{\"total\":13}");
                            request.detail("RESPONSE DURATION (ms)", "7");
                            try (var validation = request.startValidation("Validate original response")) {
                                validation.assertions(rows); validation.complete(status);
                            }
                            request.complete(status);
                        }
                    } else {
                        phase.observation(new ReportObservation("Original query", List.of(new ReportField("query id", "Q-" + index),
                                new ReportField("affected rows", "2")), List.of(new ReportObservation.Body("SQL text", "select id from sample", ReportObservation.Format.TEXT)), List.of()));
                        try (var step = phase.startAssertionStep("Original comparisons")) { step.assertions(rows); step.complete(status); }
                    }
                    phase.complete(status); scenario.complete(status);
                }
            }
        }
        return new ReportArtifactProfile(fallback ? ReportArtifactProfile.Renderer.FALLBACK : ReportArtifactProfile.Renderer.EXTENT, intended);
    }
}
