package atf.reporting.semantic;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.ApiReportTestUrls;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportSemanticSnapshotTest {
    @Test
    void extentAndFallbackEmitTheSameImmutableSemanticPopulation() throws Exception {
        Path root = Files.createTempDirectory("atf-semantic-snapshot-");
        try {
            var extent = new ExtentReportSession(root.resolve("extent.html"));
            publish(extent);
            var extentSnapshot = extent.semanticSnapshot();
            extent.close();

            var fallback = new HtmlReportSession(root.resolve("fallback.html"));
            publish(fallback);
            var fallbackSnapshot = fallback.semanticSnapshot();
            fallback.close();

            assertEquals(extentSnapshot, fallbackSnapshot);
            for (String renderer : List.of("extent.html", "fallback.html")) {
                String html = Files.readString(root.resolve(renderer));
                assertFalse(html.contains("<details class=\"step-detail phase-details\""),
                        "internal phase metadata must not create a public audit band");
                assertFalse(html.contains("1 actual API exchange captured"));
            }
            assertEquals(1, extentSnapshot.classes().size());
            assertEquals(1, extentSnapshot.scenarioCount());
            assertEquals(1, extentSnapshot.requestCount());
            assertEquals(1, extentSnapshot.validationCount());
            var extentPhase = extentSnapshot.classes().getFirst().scenarios().getFirst()
                    .phases().getFirst();
            var fallbackPhase = fallbackSnapshot.classes().getFirst().scenarios().getFirst()
                    .phases().getFirst();
            assertEquals(List.of("API audit completeness"), extentPhase.detailNames());
            assertEquals(extentPhase.detailNames(), fallbackPhase.detailNames());
            assertTrue(extentSnapshot.classes().getFirst().scenarios().getFirst()
                    .phases().getFirst().nodes().getFirst() instanceof AtfReportSemanticSnapshot.RequestSnapshot);
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void snapshotCollectionsAreImmutableAtEveryNestedLevel() {
        var recorder = new AtfReportSemanticRecorder();
        var scenario = recorder.startScenario("Tests", null, "scenario", List.of(" API ", "API", "UI"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("read", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        request.json("RESPONSE JSON");
        var validation = request.startValidation("Validate response");
        validation.assertions(List.of(new AtfAssertion("status", "200", "200", true)));
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        var snapshot = recorder.snapshot();
        assertEquals(List.of("api", "ui"), snapshot.classes().getFirst().scenarios().getFirst().tags());
        assertThrowsUnsupported(snapshot.classes());
        assertThrowsUnsupported(snapshot.classes().getFirst().scenarios());
        assertThrowsUnsupported(snapshot.classes().getFirst().scenarios().getFirst().phases());
    }

    @Test
    void lifecycleMutationProducesIndependentImmutableSnapshots() {
        var lifecycle = new AtfReportSemanticLifecycle();
        var scenario = lifecycle.startScenario("Tests", "atf.api", "scenario", List.of("api"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("read", "GET", "/users/1");

        var beforeValidation = lifecycle.snapshot();
        assertEquals(0, beforeValidation.validationCount());

        var validation = request.startValidation("Validate response");
        validation.assertions(List.of(new AtfAssertion("status", "200", "200", true)));
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        var afterValidation = lifecycle.snapshot();
        assertEquals(0, beforeValidation.validationCount());
        assertEquals(1, afterValidation.validationCount());
        assertEquals(AtfReportStatus.PASS,
                afterValidation.classes().getFirst().status());
    }

    @Test
    void sameRequestInIndependentScenariosRemainsOwnedByBothScenarios() throws Exception {
        Path report = Files.createTempDirectory("atf-scenario-request-ownership-").resolve("report.html");
        try (var session = new ExtentReportSession(report)) {
            for (String scenarioName : List.of("scenario one", "scenario two")) {
                var scenario = session.startScenario("SharedTestClass", "shared request", scenarioName,
                        List.of("api", "component"));
                var phase = scenario.startPhase("TEST BODY");
                phase.startRequest("GET /users/1 — read profile", "GET", "/users/1")
                        .complete(AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }

            var snapshot = session.semanticSnapshot();
            var scenarios = snapshot.classes().getFirst().scenarios();
            assertEquals(2, snapshot.scenarioCount());
            assertEquals(2, snapshot.requestCount(),
                    "the same local request coordinates must not deduplicate sibling scenarios");
            assertEquals(List.of("scenario one", "scenario two"),
                    scenarios.stream().map(AtfReportSemanticSnapshot.ScenarioSnapshot::name).toList());
            assertTrue(scenarios.stream().allMatch(scenario ->
                    scenario.phases().getFirst().nodes().size() == 1));
        }
        assertTrue(Files.isRegularFile(report));
    }

    @Test
    void lifecycleRejectsMutationAfterAParentOrChildScopeIsComplete() {
        var recorder = new AtfReportSemanticRecorder();
        var scenario = recorder.startScenario("Tests", "atf.api", "scenario", List.of("api"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("read", "GET", "/users/1");
        var validation = request.startValidation("Validate response");
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        assertThrows(IllegalStateException.class, () -> scenario.startPhase("TEARDOWN"));
        assertThrows(IllegalStateException.class, () -> phase.startRequest("late", "GET", "/late"));
        assertThrows(IllegalStateException.class, () -> request.json("late JSON"));
        assertThrows(IllegalStateException.class, () -> validation.complete(AtfReportStatus.PASS));
    }

    private static void publish(AtfReportPorts.Session session) {
        var scenario = session.startScenario("Tests", "scenario", List.of("api", "contract"));
        var phase = scenario.startPhase("TEST BODY");
        phase.detail("API audit completeness", "1 actual API exchange captured");
        var request = phase.startRequest("read", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        request.detail("HTTP STATUS", "200");
        request.json("REQUEST JSON", "null");
        request.json("RESPONSE JSON", "{\"id\":1}");
        request.uiActions(List.of(new AtfUiActionRecord(1, "click", "#users", "visible", "visible", 2,
                AtfReportStatus.PASS)));
        request.screenshot("report state", "base64");
        request.execution(new ReportExecutionEvidence(
                new ReportResponse(200, "ok", Map.of(), Duration.ofMillis(4),
                        ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_USER_ONE)),
                1, Duration.ofMillis(4), List.of(), List.of(),
                new ReportRequest("GET", ApiReportTestUrls.GENERIC_USER_ONE,
                        Map.of(), Map.of(), "none", ""), ReportTransportProvenance.UNKNOWN));
        var validation = request.startValidation("Validate response");
        validation.assertions(List.of(new AtfAssertion("HTTP status", "200", "200", true)));
        validation.uiActions(List.of(new AtfUiActionRecord(2, "wait", "#status", "200", "200", 1,
                AtfReportStatus.PASS)));
        validation.screenshot("validated state", "base64");
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
    }

    private static void assertThrowsUnsupported(List<?> values) {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> values.clear());
    }

    private static void deleteTree(Path root) throws Exception {
        if (Files.notExists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (java.io.IOException error) { throw new RuntimeException(error); }
            });
        }
    }
}
