package atf.reporting.ui;

import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.semantic.AtfReportSemanticSnapshot.RequestSnapshot;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static atf.reporting.evidence.ReportScreenshotPolicy.EACH_CHECKPOINT;
import static atf.reporting.evidence.ReportScreenshotPolicy.FAILURE_ONLY;
import static atf.reporting.evidence.ReportScreenshotPolicy.FINAL_AND_FAILURE;
import static atf.reporting.evidence.ReportScreenshotPolicy.FINAL_ONLY;
import static atf.reporting.evidence.ReportScreenshotPolicy.NONE;
import static atf.reporting.ui.contract.ReportUiContract.Phase.TEST_BODY;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class UiAuditLifecycleTest {
    private static final String ONE_PIXEL_PNG = ReportTestScreenshots.ONE_PIXEL_PNG;

    @ParameterizedTest
    @CsvSource({
            "false,EACH_CHECKPOINT,true, Screenshot captured; policy=each-checkpoint",
            "true,EACH_CHECKPOINT,true, Screenshot captured; policy=each-checkpoint",
            "false,FAILURE_ONLY,false, Screenshot unavailable: no capture supplied; policy=failure-only",
            "true,FAILURE_ONLY,false, Screenshot unavailable: no capture supplied; policy=failure-only",
            "false,NONE,true, Screenshot disabled by policy=none",
            "true,NONE,true, Screenshot disabled by policy=none",
            "false,FINAL_ONLY,true, Screenshot disabled by policy=final-only",
            "true,FINAL_ONLY,true, Screenshot disabled by policy=final-only"
    })
    void failureCaptureStateIsVisibleWithoutInventingBusinessAssertions(
            boolean fallback, ReportScreenshotPolicy policy, boolean supplied, String expectedState) throws Exception {
        Path report = TestReportPaths.report("failure-capture-" + (fallback ? "fallback-" : "extent-")
                + policy.configValue() + "-" + supplied + ".html");
        publishFailure(report, fallback, policy, supplied);
        String document = Files.readString(report).replaceAll("(?is)<script\\b[^>]*>.*?</script>", "");
        String html = fallback ? document.substring(document.indexOf("<section id=\"tests\""),
                document.indexOf("<section id=\"category\"")) : document;
        assertAll(
                () -> assertTrue(html.contains(expectedState)),
                () -> assertFalse(html.contains("raw-capture-secret")),
                () -> assertTrue(html.contains("Failure evidence")),
                () -> assertTrue(html.contains("Click") && html.contains("button[data-test=checkout]")),
                () -> assertTrue(html.contains("IllegalStateException: checkout was rejected")),
                () -> assertEquals(1, occurrences(html, ">Failure cause<")),
                () -> assertEquals(1, occurrences(html, ">Click<")),
                () -> assertEquals(1, occurrences(html, ">Screenshot<")),
                () -> assertEquals(1, occurrences(html, ">THEN checkout fails<")),
                () -> assertEquals(supplied && policy.captures(ReportScreenshotPolicy.CapturePoint.FAILURE),
                        html.contains("data-atf-screenshot=\"base64\"")),
                () -> assertFalse(html.contains("Audit details") || html.contains("Request details")
                        || html.contains("Response details") || html.contains("Execution evidence")));
        if (!fallback) assertTrue(html.contains("status=\"fail\""));
        else assertTrue(html.contains("status-fail"));
    }

    private static void publishFailure(Path report, boolean fallback,
                                       ReportScreenshotPolicy policy, boolean supplied) {
        try (AtfReportPorts.Session session = fallback
                ? new HtmlReportSession(report) : new ExtentReportSession(report)) {
            var scenario = session.startScenario("FailureCaptureContract", "Failure capture proof", List.of("ui", "component"));
            var phase = scenario.startPhase("TEST BODY");
            new UiEvidencePublisher(phase, policy).publishFailure("checkout fails",
                    "button[data-test=checkout]?access_token=raw-capture-secret",
                    List.of(new AtfUiActionRecord(41, "Click", "button[data-test=checkout]",
                            "checkout opens", "checkout was rejected", 7, AtfReportStatus.FAIL)),
                    new IllegalStateException("checkout was rejected"), supplied ? ONE_PIXEL_PNG : null);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }
    }

    private static int occurrences(String html, String token) {
        return html.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    @Test
    void publishesExecutedActionsBeforeClosingTheScenarioOnFailure(@TempDir Path root) throws Exception {
        Path report;
        try (var session = UiExtentReportSession.openConfigured(
                root, "failure-boundary.html", AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiAuditLifecycleTest",
                    "atf.reporting.ui", "failure boundary", List.of("ui", "failure"));
            var body = scenario.startPhase("TEST BODY");
            new UiAuditLifecycle(scenario, body).fail(
                    "UI exploration failed", "#test-view",
                    List.of(new AtfUiActionRecord(1, "Click", "#test-view",
                            "Tests is visible", "click completed", 7, AtfReportStatus.PASS)),
                    new IllegalStateException("synthetic failure"), ONE_PIXEL_PNG);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("ui-failure")),
                () -> assertTrue(html.contains("Click") && html.contains("#test-view")),
                () -> assertTrue(html.contains("Failure cause") && html.contains("synthetic failure")),
                () -> assertTrue(html.contains("UI failure screenshot")),
                () -> assertTrue(html.contains("data:image/png;base64,")),
                () -> assertTrue(html.contains("status=\"fail\"")));
    }

    @Test
    void preservesActionsWhenFailureHasNoScreenshot(@TempDir Path root) throws Exception {
        Path report;
        try (var session = UiExtentReportSession.openConfigured(
                root, "failure-without-screenshot.html", AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiAuditLifecycleTest",
                    "atf.reporting.ui", "failure without screenshot", List.of("ui", "failure"));
            var body = scenario.startPhase("TEST BODY");
            new UiAuditLifecycle(scenario, body).fail(
                    "UI exploration failed", "#test-view",
                    List.of(new AtfUiActionRecord(1, "Click", "#test-view",
                            "Tests is visible", "click completed", 7, AtfReportStatus.PASS)),
                    new IllegalStateException("capture unavailable"));
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("capture unavailable")),
                () -> assertTrue(html.contains("Click") && html.contains("#test-view")),
                () -> assertTrue(html.contains("Screenshot unavailable: no capture supplied")),
                () -> assertTrue(!html.contains("data-atf-screenshot=\"base64\"")),
                () -> assertTrue(html.contains("status=\"fail\"")));
    }
    @ParameterizedTest(name = "{0}: {1}, screenshot={4}")
    @MethodSource("captureCases")
    void captureStateSurvivesBothRenderersWithoutFakeHttpOrDuplicateActions(
            Renderer renderer, ReportScreenshotPolicy policy, String screenshot,
            String expectedReason, boolean expectedImage, @TempDir Path directory) throws Exception {
        Path report = directory.resolve("capture-state.html");
        AtfReportSemanticSnapshot snapshot;
        try (var session = renderer.open(report)) {
            publishFailure(session, policy, screenshot, "capture state", actions());
            snapshot = renderer.snapshot(session);
        }

        String html = Files.readString(report);
        String markup = publicMarkup(html);
        var scenario = snapshot.classes().getFirst().scenarios().getFirst();
        var phase = scenario.phases().getFirst();
        var request = assertInstanceOf(RequestSnapshot.class, phase.nodes().getFirst());
        var validation = request.validations().getFirst();
        assertAll(
                () -> assertEquals(AtfReportStatus.FAIL, scenario.status()),
                () -> assertEquals(1, phase.nodes().size(), "one Gherkin checkpoint owns the failure"),
                () -> assertEquals(1, request.validations().size()),
                () -> assertEquals(List.of("Failure cause"), validation.assertionNames()),
                () -> assertEquals(3, validation.uiActionCount(), "one action, one primary diagnostic and one capture observation"),
                () -> assertEquals(expectedImage ? 1 : 0, validation.screenshotCount()),
                () -> assertTrue(markup.contains(expectedReason)),
                () -> assertTrue(html.contains("Click") && html.contains("#test-view")),
                () -> assertTrue(html.contains("synthetic failure") && html.contains("Failure evidence")),
                () -> assertEquals(expectedImage, html.contains("data:image/png;base64," + ONE_PIXEL_PNG)),
                () -> assertFalse(markup.contains("Request details")),
                () -> assertFalse(markup.contains("Response details")),
                () -> assertFalse(markup.contains("Audit details")),
                () -> assertFalse(markup.contains("EVIDENCE POLICY")));
    }

    @ParameterizedTest
    @EnumSource(Renderer.class)
    void screenshotWithoutExecutedActionsDoesNotInventAPlaywrightSequence(
            Renderer renderer, @TempDir Path directory) throws Exception {
        Path report = directory.resolve("capture-without-actions.html");
        try (var session = renderer.open(report)) {
            publishFailure(session, EACH_CHECKPOINT, ONE_PIXEL_PNG, "no executed actions", List.of());
        }
        String markup = publicMarkup(Files.readString(report));
        assertAll(
                () -> assertTrue(markup.contains("Screenshot captured; policy=each-checkpoint")),
                () -> assertTrue(markup.contains("href=\"data:image/png;base64," + ONE_PIXEL_PNG)),
                () -> assertFalse(markup.contains("atf-ui-action-sequence")));
    }

    @Test
    void publishesInspectableCaptureMatrix() throws Exception {
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.CLIENT, "Fixture adapter; token=synthetic-capture-secret")
                .build();
        String fileName = "ui-reporting-failure-capture.html";
        var options = UiExtentReportOptions.builder(TestReportPaths.report(fileName).getParent())
                .fileName(fileName)
                .console(AtfConsoleSink.noOp()).environment(environment).build();
        Path report;
        try (var session = UiExtentReportSession.openConfigured(options)) {
            report = session.reportPath();
            auditFailure(session, EACH_CHECKPOINT, ONE_PIXEL_PNG, "capture is available");
            auditFailure(session, EACH_CHECKPOINT, null, "capture is unavailable");
            auditFailure(session, NONE, ONE_PIXEL_PNG, "capture is disabled by policy");
            auditFailure(session, EACH_CHECKPOINT, "not base64", "capture payload is invalid");
            session.startScenario("CaptureAvailabilityContract", "atf.reporting.ui",
                    "unrelated passing sibling", List.of("component", "passing"))
                    .complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Screenshot captured; policy=each-checkpoint")),
                () -> assertTrue(html.contains("Screenshot unavailable: no capture supplied; policy=each-checkpoint")),
                () -> assertTrue(html.contains("Screenshot disabled by policy=none")),
                () -> assertTrue(html.contains("Screenshot unavailable: invalid Base64 PNG supplied; policy=each-checkpoint")),
                () -> assertFalse(html.contains("data:image/png;base64,not base64")),
                () -> assertTrue(html.contains("Fixture adapter; token=***REDACTED***")),
                () -> assertFalse(html.contains("synthetic-capture-secret")));
    }

    private static void auditFailure(UiExtentReportSession session, ReportScreenshotPolicy policy,
                                     String screenshot, String title) {
        var scenario = session.startScenario("CaptureAvailabilityContract", "atf.reporting.ui",
                title + " — expected synthetic failure", List.of("component", "expected-failure"),
                ReportEvidenceProfile.forScreenshotPolicy(policy));
        new UiAuditLifecycle(scenario, scenario.startPhase(TEST_BODY.displayName()))
                .fail("the synthetic failure preserves its evidence", "#test-view", actions(),
                        new IllegalStateException("synthetic failure"), screenshot);
    }

    private static void publishFailure(AtfReportPorts.Session session, ReportScreenshotPolicy policy,
                                        String screenshot, String title, List<AtfUiActionRecord> executedActions) {
        var scenario = session.startScenario("CaptureAvailabilityContract", title, List.of("component"));
        var phase = scenario.startPhase(TEST_BODY.displayName());
        new UiEvidencePublisher(phase, policy).publishFailure(title, "#test-view", executedActions,
                new IllegalStateException("synthetic failure"), screenshot);
        phase.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private static List<AtfUiActionRecord> actions() {
        return List.of(new AtfUiActionRecord(1, "Click", "#test-view", "Tests is visible",
                "click completed", 7, AtfReportStatus.PASS));
    }

    /** Public markup, excluding vocabulary strings inside the embedded runtime and styles. */
    private static String publicMarkup(String html) {
        return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
    }

    private static Stream<Arguments> captureCases() {
        return Stream.of(Renderer.values()).flatMap(renderer -> Stream.of(
                Arguments.of(renderer, EACH_CHECKPOINT, ONE_PIXEL_PNG, "Screenshot captured; policy=each-checkpoint", true),
                Arguments.of(renderer, EACH_CHECKPOINT, null, "Screenshot unavailable: no capture supplied; policy=each-checkpoint", false),
                Arguments.of(renderer, EACH_CHECKPOINT, "  ", "Screenshot unavailable: no capture supplied; policy=each-checkpoint", false),
                Arguments.of(renderer, EACH_CHECKPOINT, "not base64", "Screenshot unavailable: invalid Base64 PNG supplied; policy=each-checkpoint", false),
                Arguments.of(renderer, EACH_CHECKPOINT, "bm90IGEgcG5n", "Screenshot unavailable: invalid Base64 PNG supplied; policy=each-checkpoint", false),
                Arguments.of(renderer, NONE, ONE_PIXEL_PNG, "Screenshot disabled by policy=none", false),
                Arguments.of(renderer, NONE, null, "Screenshot disabled by policy=none", false),
                Arguments.of(renderer, FINAL_ONLY, ONE_PIXEL_PNG, "Screenshot disabled by policy=final-only", false),
                Arguments.of(renderer, FAILURE_ONLY, ONE_PIXEL_PNG, "Screenshot captured; policy=failure-only", true),
                Arguments.of(renderer, FINAL_AND_FAILURE, ONE_PIXEL_PNG, "Screenshot captured; policy=final-and-failure", true),
                Arguments.of(renderer, FINAL_AND_FAILURE, null, "Screenshot unavailable: no capture supplied; policy=final-and-failure", false)));
    }

    private enum Renderer {
        EXTENT, FALLBACK;

        AtfReportPorts.Session open(Path path) {
            return this == EXTENT ? new ExtentReportSession(path) : new HtmlReportSession(path);
        }

        AtfReportSemanticSnapshot snapshot(AtfReportPorts.Session session) {
            return session instanceof ExtentReportSession extent ? extent.semanticSnapshot()
                    : ((HtmlReportSession) session).semanticSnapshot();
        }
    }
}
