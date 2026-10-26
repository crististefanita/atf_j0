package atf.reporting.ui.validation;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Executable;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Owner-module guard and real renderer proof; no application fixture or external HTTP. */
@AtfComponentTest
class UiApiValidationContractComponentTest {
    enum Mode { CHECKPOINT, SETUP, FINAL, UNIFIED }

    @Test
    void setupRejectsAMissingTypedModelBeforeCreatingARequest() throws Exception {
        var requested = TestReportPaths.report("typed-api-missing-setup-model.html");
        try (var session = UiExtentReportSession.openConfigured(requested.getParent(),
                requested.getFileName().toString(), AtfConsoleSink.noOp())) {
            var scenario = session.startScenario("TypedApiContract", getClass().getPackageName(),
                    "missing API model is not an empty API channel", List.of("component"));
            var phase = scenario.startPhase("SETUP");
            assertThrows(NullPointerException.class, () -> phase.setup(THEN, "setup", "/setup",
                    List.of(), List.of(), null, AtfReportStatus.PASS));
            assertTrue(session.semanticSnapshot().classes().getFirst().scenarios().getFirst()
                    .phases().getFirst().nodes().isEmpty());
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }

    @Test
    void publicValidationEntrypointsCannotAcceptRawExchangeCollections() {
        var declaredMethods = Arrays.stream(UiExtentReportSession.UiPhase.class.getDeclaredMethods()).toList();
        assertFalse(declaredMethods.stream().anyMatch(method -> method.getName().equals("screenValidation")),
                "The report API must expose UI validation semantics, not a separate screen-validation route");
        var methods = declaredMethods.stream()
                .filter(method -> List.of("step", "setup", "uiValidation", "apiTrace")
                        .contains(method.getName())).toList();
        assertFalse(methods.isEmpty());
        assertTrue(methods.stream().anyMatch(method -> method.getName().equals("uiValidation")));
        methods.forEach(UiApiValidationContractComponentTest::assertNoRawExchangeParameter);
        assertEquals(1, methods.stream().filter(method -> method.getName().equals("step"))
                .filter(method -> Arrays.asList(method.getParameterTypes())
                        .contains(UiApiValidationEvidence.class)).count());
    }

    @Test
    void publisherLifecycleAndUnifiedBufferCannotReintroduceTheLegacyRoute() throws Exception {
        for (String name : List.of("UiEvidencePublisher", "UiPhaseLifecycle", "UiUnifiedFlowCheckpoint")) {
            Class<?> owner = Class.forName("atf.reporting.ui." + name);
            Arrays.stream(owner.getDeclaredMethods())
                    .filter(method -> !method.getName().equals("networkInspector"))
                    .forEach(UiApiValidationContractComponentTest::assertNoRawExchangeParameter);
            Arrays.stream(owner.getDeclaredConstructors())
                    .forEach(UiApiValidationContractComponentTest::assertNoRawExchangeParameter);
        }
    }

    @Test
    void typedExchangeVerdictsNeverLeakBetweenSiblingsOrParentCorrelations() {
        var passed = new UiApiValidationEvidence.Exchange(exchange("/passed"), List.of(check("API pass", true)));
        var failed = new UiApiValidationEvidence.Exchange(exchange("/failed"), List.of(check("API fail", false)));
        var model = new UiApiValidationEvidence(List.of(check("correlation", true)), List.of(passed, failed));
        assertEquals(AtfReportStatus.PASS, passed.status());
        assertEquals(AtfReportStatus.FAIL, failed.status());
        assertEquals(AtfReportStatus.FAIL, model.status());
        assertTrue(UiApiValidationEvidence.empty().isEmpty());
        assertEquals(AtfReportStatus.INFO, UiApiValidationEvidence.empty().status());
    }

    @ParameterizedTest
    @CsvSource({"CHECKPOINT,true", "CHECKPOINT,false", "SETUP,true", "SETUP,false",
            "FINAL,true", "FINAL,false", "UNIFIED,true", "UNIFIED,false"})
    void everyPublicationPathKeepsUiAndApiOwnershipAndCanonicalExchangeMarkup(Mode mode, boolean uiPass)
            throws Exception {
        var requested = TestReportPaths.report("typed-api-" + mode.name().toLowerCase()
                + "-ui-" + uiPass + ".html");
        var profile = ReportEvidenceProfile.builder()
                .flowMode(mode == Mode.UNIFIED ? ReportEvidenceFlowMode.UNIFIED_FLOW
                        : ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(ReportScreenshotPolicy.NONE).build();
        var api = new UiApiValidationEvidence(List.of(check("API correlation", true)), List.of(
                new UiApiValidationEvidence.Exchange(exchange("/first"), List.of(check("First API check", true))),
                new UiApiValidationEvidence.Exchange(exchange("/second"),
                        List.of(check("Second API check", !uiPass)))));
        var uiStatus = uiPass ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        java.nio.file.Path report;
        try (var session = UiExtentReportSession.openConfigured(requested.getParent(),
                requested.getFileName().toString(), AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("TypedApiContract", getClass().getPackageName(),
                    mode + ": independent UI and API verdicts", List.of("component", "ui", "api"));
            var phase = scenario.startPhase(mode == Mode.SETUP ? "SETUP" : "TEST BODY");
            var uiChecks = List.of(check("UI check", uiPass));
            switch (mode) {
                case CHECKPOINT, UNIFIED -> phase.step(THEN, "the checkpoint owns both channels",
                        "/screen", "", uiChecks, List.of(), api,
                        ReportScreenshotPolicy.CapturePoint.CHECKPOINT, uiStatus);
                case SETUP -> phase.setup(THEN, "the setup owns its API checks", "/setup",
                        uiChecks, List.of(), api, uiStatus);
                case FINAL -> phase.uiValidation("the terminal checkpoint owns both channels", "/screen",
                        "", uiChecks, List.of(), api, ReportScreenshotPolicy.CapturePoint.FINAL, uiStatus);
            }
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            var request = (AtfReportSemanticSnapshot.RequestSnapshot) session.semanticSnapshot()
                    .classes().getFirst().scenarios().getFirst().phases().getFirst().nodes().getFirst();
            var apiNode = request.validations().stream().filter(node -> node.title().equals("API validation"))
                    .findFirst().orElseThrow();
            assertEquals(api.status(), apiNode.status());
            assertEquals(ReportEvidenceOwner.API, apiNode.evidenceOwner());
            assertTrue(apiNode.requests().stream().flatMap(exchange -> exchange.validations().stream())
                    .allMatch(validation -> validation.evidenceOwner() == ReportEvidenceOwner.API));
            assertEquals(List.of("API correlation"), apiNode.assertionNames());
            assertEquals(List.of(AtfReportStatus.PASS, !uiPass ? AtfReportStatus.PASS : AtfReportStatus.FAIL),
                    apiNode.requests().stream().map(AtfReportSemanticSnapshot.RequestSnapshot::status).toList());
            assertFalse(apiNode.assertionNames().contains("UI check"));
            if (mode != Mode.SETUP) {
                var uiNode = request.validations().stream().filter(node -> node.title().contains("validation")
                        && !node.title().equals("API validation")).findFirst().orElseThrow();
                assertEquals(uiStatus, uiNode.status());
                assertEquals(ReportEvidenceOwner.UI, uiNode.evidenceOwner());
                assertEquals(List.of("UI check"), uiNode.assertionNames());
            }
            assertEquals(AtfReportStatus.FAIL, request.status());
        }
        String html = Files.readString(report);
        assertTrue(html.contains("Request details"));
        assertTrue(html.contains("Response details"));
        assertTrue(html.contains("Execution evidence"));
        assertTrue(html.contains("GET /first"));
        assertTrue(html.contains("GET /second"));
        assertFalse(html.contains("API 1 · METHOD"));
        assertFalse(html.contains("Runtime diagnostic"));
    }

    private static void assertNoRawExchangeParameter(Executable executable) {
        assertFalse(Arrays.stream(executable.getGenericParameterTypes())
                        .anyMatch(type -> type.getTypeName().contains(ReportExecutionEvidence.class.getName())),
                () -> "Raw exchange publication is forbidden: " + executable);
    }

    private static AtfAssertion check(String title, boolean passed) {
        return new AtfAssertion(title, "expected", passed ? "expected" : "different", passed);
    }

    private static ReportExecutionEvidence exchange(String path) {
        var request = new ReportRequest("GET", path, Map.of(), Map.of("Accept", "application/json"),
                "none", "");
        var response = new ReportResponse(200, "{\"ok\":true}", Map.of("Content-Type", "application/json"),
                Duration.ofMillis(3), "https://typed-fixture.invalid" + path);
        return new ReportExecutionEvidence(response, 1, Duration.ofMillis(3), List.of(), List.of(),
                request, ReportTransportProvenance.ACCEPTANCE_MOCK);
    }
}
