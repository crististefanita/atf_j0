package atf.reporting.ui.profile;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceCaptureProvider;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the typed consumer profile crosses the session boundary without a browser. */
@AtfIntegrationTest
class ReportEvidenceProfileIntegrationTest {
    @Test
    void crossPhaseFlowKeepsSetupBeforeBodyWithOwnedApiEvidenceAndVerdicts() throws Exception {
        Path root = Files.createTempDirectory("atf-cross-phase-flow-");
        var profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .build();
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "cross-phase.html",
                AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("CrossPhaseTest", "atf.tests", "authenticated purchase", List.of());
            scenario.unifyAcrossPhases();
            var setup = scenario.startPhase("SETUP");
            setup.setup(AtfGherkinKeyword.GIVEN, "account is ready", "/login",
                    List.of(new AtfAssertion("Account", "ready", "ready", true)),
                    List.of(new AtfUiActionRecord(1, "Fill account", "#email", "email entered",
                            "entered", 2, AtfReportStatus.PASS)), observedExchanges(List.of(paymentEvidence())),
                    AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase("TEST BODY");
            body.uiValidation("purchase is complete", "/confirmation",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Order", "confirmed", "confirmed", true)),
                    List.of(new AtfUiActionRecord(2, "Confirm purchase", "#confirm",
                            "order placed", "clicked", 3, AtfReportStatus.PASS)),
                    ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
            body.completeFinal(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(1, count(html, "<td>EVIDENCE TYPE</td><td>unified-ui-flow</td>")),
                () -> assertEquals(1, count(html, "<span>THEN purchase is complete</span>")),
                () -> assertTrue(html.indexOf("SETUP · Setup validation") >= 0
                        && html.indexOf("TEST BODY · UI validation")
                                > html.indexOf("SETUP · Setup validation")),
                () -> assertTrue(html.indexOf("Fill account") >= 0
                        && html.indexOf("SETUP · API validation") > html.indexOf("Fill account")
                        && html.indexOf("TEST BODY · UI validation")
                                > html.indexOf("SETUP · API validation")
                        && html.indexOf("Confirm purchase")
                                > html.indexOf("TEST BODY · UI validation")),
                () -> assertTrue(html.contains("FLOW CHECKPOINT 1 PHASE")),
                () -> assertTrue(html.contains("FLOW CHECKPOINT 2 PHASE")),
                () -> assertTrue(html.contains("Exchange 1")),
                () -> assertTrue(html.contains("Account")),
                () -> assertTrue(html.contains("Order")),
                () -> assertEquals(1, inlineScreenshots(html)));
    }

    @Test
    void failedSetupPublishesItsFailureWithoutInventingBodyOrTerminalScreenshot() throws Exception {
        Path root = Files.createTempDirectory("atf-cross-phase-setup-failure-");
        var profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .build();
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "setup-failure.html",
                AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("CrossPhaseTest", "atf.tests", "failed account", List.of());
            scenario.unifyAcrossPhases();
            var setup = scenario.startPhase("SETUP");
            setup.setup(AtfGherkinKeyword.GIVEN, "account is ready", "/login",
                    List.of(new AtfAssertion("Login", "accepted", "rejected", false)),
                    AtfReportStatus.FAIL);
            setup.completeFinal(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }
        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(1, count(html, "<td>EVIDENCE TYPE</td><td>unified-ui-flow</td>")),
                () -> assertTrue(html.contains("SETUP · Setup validation")),
                () -> assertTrue(html.contains("rejected")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"fail\"")),
                () -> assertFalse(html.contains("<span>TEST BODY</span>")),
                () -> assertEquals(0, inlineScreenshots(html)));
    }

    @Test
    void nullScenarioProfileIsRejectedBeforeAnExtentScenarioIsCreated() throws Exception {
        Path root = Files.createTempDirectory("atf-null-scenario-profile-");
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "null-profile.html",
                AtfConsoleSink.noOp(), ReportEvidenceProfile.forScreenshotPolicy(
                        ReportScreenshotPolicy.FINAL_ONLY))) {
            report = session.reportPath();
            assertThrows(NullPointerException.class, () -> session.startScenario(
                    "ProfileTest", "atf.reporting.ui", "must not appear", List.of(), null));
            var valid = session.startScenario("ProfileTest", "atf.reporting.ui",
                    "subsequent valid scenario", List.of());
            valid.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertFalse(html.contains("must not appear"));
        assertTrue(html.contains("subsequent valid scenario"));
    }

    @Test
    void unifiedFlowPublishesOneBusinessNodeWithNestedEvidenceAndOnlyTheFinalScreenshot()
            throws Exception {
        Path root = Files.createTempDirectory("atf-evidence-profile-");
        ReportEvidenceProfile profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .captureSource(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK)
                .build();

        Path report;
        ReportEvidenceCaptureProvider provider = new ReportEvidenceCaptureProvider() {
            @Override public ReportEvidenceCaptureSource source() {
                return ReportEvidenceCaptureSource.DETERMINISTIC_MOCK;
            }
            @Override public ReportScreenshot capture(String title, String target) {
                return new ReportScreenshot(title, ReportTestScreenshots.SECOND_PIXEL_PNG);
            }
        };
        try (var session = UiExtentReportSession.openConfigured(root, "profile.html",
                AtfConsoleSink.noOp(), profile)) {
            assertEquals(profile, session.evidenceProfile());
            var scenario = session.startScenario("ProfileTest", "atf.reporting.ui",
                    "typed checkout", List.of("evidence", "component"));
            var body = scenario.startPhase("TEST BODY");
            var apiEvidence = paymentEvidence();
            body.step(AtfGherkinKeyword.GIVEN, "payment is ready", "#checkout",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Payment form", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(1, "Get text", "#payment-title",
                                    "payment heading", "Payment", 4, AtfReportStatus.PASS),
                            new AtfUiActionRecord(1, "Visible text", "#payment-title",
                                    "Payment", "Payment", 2, AtfReportStatus.PASS,
                                    AtfUiEvidenceSource.OBSERVE)),
                    observedExchanges(List.of(apiEvidence)), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            body.step(AtfGherkinKeyword.WHEN, "checkout is submitted", "#checkout",
                    null, List.of(), List.of(), observedExchanges(List.of(apiEvidence)), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            var finalCapture = profile.capture(ReportScreenshotPolicy.CapturePoint.FINAL,
                    "confirmation is visible", "#confirmation", provider).orElseThrow();
            body.uiValidation(finalCapture.title(), "#confirmation", finalCapture.base64Png(),
                    List.of(new AtfAssertion("Confirmation", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(2, "Get text", "#confirmation",
                            "confirmation message", "Order confirmed", 5, AtfReportStatus.PASS)),
                    ReportScreenshotPolicy.CapturePoint.FINAL,
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            report = session.reportPath();
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(1, count(html,
                        "<td>EVIDENCE TYPE</td><td>unified-ui-flow</td>")),
                () -> assertEquals(1, count(html,
                        "<span>THEN confirmation is visible</span>")),
                () -> assertTrue(html.contains("<td>FLOW SCENARIO</td><td>typed checkout</td>")),
                () -> assertFalse(html.contains("Checkpoint 1")),
                () -> assertFalse(html.contains("Checkpoint 2")),
                () -> assertFalse(html.contains("GIVEN payment is ready")),
                () -> assertFalse(html.contains("WHEN checkout is submitted")),
                () -> assertFalse(html.contains("FLOW CHECKPOINT COUNT")),
                () -> assertEquals(1, count(html, "API validation")),
                () -> assertTrue(html.contains("Exchange 1")),
                () -> assertTrue(html.contains("Exchange 2")),
                () -> assertTrue(html.contains("Get text")),
                () -> assertTrue(html.contains("Payment form")),
                () -> assertTrue(html.contains("Payment")),
                () -> assertTrue(html.contains("Order confirmed")),
                () -> assertEquals(1, count(html, "UI action sequence (2 entries)")),
                () -> assertTrue(html.contains("Audit observations · 1 checkpoint")),
                () -> assertTrue(html.contains("class=\"atf-ui-observations\"")),
                () -> assertEquals(1, inlineScreenshots(html)),
                () -> assertTrue(html.contains("href=\"data:image/png;base64,"
                        + ReportTestScreenshots.SECOND_PIXEL_PNG + "\"")),
                () -> assertFalse(html.contains("href=\"data:image/png;base64,"
                        + ReportTestScreenshots.ONE_PIXEL_PNG + "\"")));
    }

    @Test
    void unifiedFlowPreservesTypedApiExchangeVerdictsWithoutFailingUiValidation() throws Exception {
        Path root = Files.createTempDirectory("atf-unified-owned-verdicts-");
        ReportEvidenceProfile profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .captureSource(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK)
                .build();
        Path report;
        var apiEvidence = new UiApiValidationEvidence(List.of(
                new AtfAssertion("cross-channel correlation", "same order", "same order", true)),
                List.of(
                        new UiApiValidationEvidence.Exchange(paymentEvidence(),
                                List.of(new AtfAssertion("payment exchange contract", "valid", "invalid", false))),
                        new UiApiValidationEvidence.Exchange(paymentEvidence(),
                                List.of(new AtfAssertion("order exchange contract", "valid", "valid", true)))));

        try (var session = UiExtentReportSession.openConfigured(root, "typed-unified.html",
                AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("ProfileTest", "atf.reporting.ui",
                    "typed unified verdicts", List.of("evidence", "component"));
            var body = scenario.startPhase("TEST BODY");
            body.step(AtfGherkinKeyword.THEN, "the purchase is confirmed", "#confirmation", null,
                    List.of(new AtfAssertion("screen remains valid", "confirmation visible",
                            "confirmation visible", true)), List.of(), apiEvidence,
                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        int ui = html.indexOf("UI validation");
        int api = html.indexOf("API validation");
        assertAll(
                () -> assertTrue(ui >= 0 && api > ui),
                () -> assertTrue(html.contains("screen remains valid")),
                () -> assertTrue(html.contains("cross-channel correlation")),
                () -> assertTrue(html.contains("payment exchange contract")),
                () -> assertTrue(html.contains("order exchange contract")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"pass\"")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"fail\"")));
    }

    @Test
    void screenCheckpointsKeepTheirExistingIndependentGherkinNodes() throws Exception {
        Path root = Files.createTempDirectory("atf-evidence-checkpoints-");
        ReportEvidenceProfile profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                .captureSource(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK)
                .build();
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "checkpoints.html",
                AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("ProfileTest", "atf.reporting.ui",
                    "independent checkpoints", List.of("evidence", "component"));
            var body = scenario.startPhase("TEST BODY");
            body.step(AtfGherkinKeyword.GIVEN, "the page is open", "#page",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Page", "open", "open", true)),
                    List.of(new AtfUiActionRecord(1, "Navigate", "/start", "page is loaded",
                            "document loaded", 8, AtfReportStatus.PASS)),
                    UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT,
                    AtfReportStatus.PASS);
            body.step(AtfGherkinKeyword.THEN, "the result is visible", "#result",
                    ReportTestScreenshots.SECOND_PIXEL_PNG,
                    List.of(new AtfAssertion("Result", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(2, "Get text", "#result", "read result",
                            "visible", 3, AtfReportStatus.PASS)),
                    UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT,
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(0, count(html, "unified-ui-flow")),
                () -> assertEquals(1, count(html, "<span>GIVEN the page is open</span>")),
                () -> assertEquals(1, count(html, "<span>THEN the result is visible</span>")),
                () -> assertTrue(html.contains("GIVEN the page is open")),
                () -> assertTrue(html.contains("THEN the result is visible")),
                () -> assertEquals(2, count(html, "class=\"atf-ui-action-table\"")),
                () -> assertEquals(2, inlineScreenshots(html)),
                () -> assertTrue(html.indexOf("GIVEN the page is open")
                        < html.indexOf("Navigate")),
                () -> assertTrue(html.indexOf("Navigate")
                        < html.indexOf(ReportTestScreenshots.ONE_PIXEL_PNG)),
                () -> assertTrue(html.indexOf(ReportTestScreenshots.ONE_PIXEL_PNG)
                        < html.indexOf("THEN the result is visible")),
                () -> assertTrue(html.indexOf("THEN the result is visible")
                        < html.indexOf("Get text")),
                () -> assertTrue(html.indexOf("Get text")
                        < html.indexOf(ReportTestScreenshots.SECOND_PIXEL_PNG)));
    }

    @Test
    void sharedReportKeepsUnifiedFinalOnlyAndCheckpointProfilesIsolatedPerScenario() throws Exception {
        Path root = Files.createTempDirectory("atf-mixed-scenario-profiles-");
        ReportEvidenceProfile sessionProfile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                .captureSource(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK)
                .build();
        ReportEvidenceProfile unifiedFinalOnly = sessionProfile.toBuilder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .build();
        ReportEvidenceProfile independentCheckpoints = sessionProfile.toBuilder()
                .flowMode(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                .build();
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "mixed-profiles.html",
                AtfConsoleSink.noOp(), sessionProfile)) {
            report = session.reportPath();

            var finalOnlyScenario = session.startScenario("MixedProfileTest", "atf.reporting.ui",
                    "final-only scenario", List.of("evidence"), unifiedFinalOnly);
            var finalOnlyBody = finalOnlyScenario.startPhase("TEST BODY");
            finalOnlyBody.step(AtfGherkinKeyword.GIVEN, "initial state is ready", "#start",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Initial state", "ready", "ready", true)),
                    List.of(new AtfUiActionRecord(1, "Navigate", "/start", "page loaded",
                            "document loaded", 5, AtfReportStatus.PASS)), UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            finalOnlyBody.uiValidation("terminal state is confirmed", "#confirmation",
                    ReportTestScreenshots.SECOND_PIXEL_PNG,
                    List.of(new AtfAssertion("Confirmation", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(2, "Get text", "#confirmation", "read confirmation",
                            "Order confirmed", 3, AtfReportStatus.PASS)),
                    ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
            finalOnlyBody.complete(AtfReportStatus.PASS);
            finalOnlyScenario.complete(AtfReportStatus.PASS);

            var checkpointScenario = session.startScenario("MixedProfileTest", "atf.reporting.ui",
                    "checkpoint scenario", List.of("evidence"), independentCheckpoints);
            var checkpointBody = checkpointScenario.startPhase("TEST BODY");
            checkpointBody.step(AtfGherkinKeyword.GIVEN, "checkpoint flow starts", "#start",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Start", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(1, "Navigate", "/start", "page loaded",
                            "document loaded", 5, AtfReportStatus.PASS)), UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            checkpointBody.step(AtfGherkinKeyword.THEN, "checkpoint result is visible", "#result",
                    ReportTestScreenshots.SECOND_PIXEL_PNG,
                    List.of(new AtfAssertion("Result", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(2, "Get text", "#result", "read result",
                            "visible", 3, AtfReportStatus.PASS)), UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            checkpointBody.complete(AtfReportStatus.PASS);
            checkpointScenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(1, count(html, "<td>EVIDENCE TYPE</td><td>unified-ui-flow</td>")),
                () -> assertTrue(html.contains("<td>FLOW SCENARIO</td><td>final-only scenario</td>")),
                () -> assertTrue(html.contains("<span>THEN terminal state is confirmed</span>")),
                () -> assertFalse(html.contains("GIVEN initial state is ready")),
                () -> assertTrue(html.contains("<span>GIVEN checkpoint flow starts</span>")),
                () -> assertTrue(html.contains("<span>THEN checkpoint result is visible</span>")),
                () -> assertEquals(3, count(html, "class=\"atf-ui-action-table\"")),
                // The shared asset manifest stores the two distinct PNG payloads once each.
                () -> assertEquals(2, inlineScreenshots(html)),
                () -> assertEquals(1, count(html, ReportTestScreenshots.ONE_PIXEL_PNG)),
                () -> assertEquals(1, count(html, ReportTestScreenshots.SECOND_PIXEL_PNG)));
    }

    @Test
    void sharedReportKeepsProfilesIsolatedWhenCheckpointScenarioStartsFirst() throws Exception {
        Path root = Files.createTempDirectory("atf-reversed-scenario-profiles-");
        ReportEvidenceProfile sessionProfile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                .captureSource(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK)
                .build();
        ReportEvidenceProfile finalOnly = sessionProfile.toBuilder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .build();
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "reversed-profiles.html",
                AtfConsoleSink.noOp(), sessionProfile)) {
            report = session.reportPath();

            var checkpointScenario = session.startScenario("MixedProfileTest", "atf.reporting.ui",
                    "checkpoint-first", List.of("evidence"), sessionProfile);
            var checkpointBody = checkpointScenario.startPhase("TEST BODY");
            checkpointBody.step(AtfGherkinKeyword.GIVEN, "checkpoint state is ready", "#start",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Start page", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(1, "Navigate", "/start", "page loaded",
                            "document loaded", 5, AtfReportStatus.PASS)), UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            checkpointBody.step(AtfGherkinKeyword.THEN, "checkpoint result appears", "#result",
                    ReportTestScreenshots.SECOND_PIXEL_PNG,
                    List.of(new AtfAssertion("Result", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(2, "Get text", "#result", "read result",
                            "visible", 3, AtfReportStatus.PASS)), UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            checkpointBody.complete(AtfReportStatus.PASS);
            checkpointScenario.complete(AtfReportStatus.PASS);

            var finalScenario = session.startScenario("MixedProfileTest", "atf.reporting.ui",
                    "final-only-second", List.of("evidence"), finalOnly);
            var finalBody = finalScenario.startPhase("TEST BODY");
            finalBody.step(AtfGherkinKeyword.GIVEN, "discarded initial state", "#start",
                    ReportTestScreenshots.ONE_PIXEL_PNG, List.of(),
                    List.of(new AtfUiActionRecord(1, "Navigate", "/start", "page loaded",
                            "document loaded", 5, AtfReportStatus.PASS)), UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            finalBody.uiValidation("terminal state appears", "#confirmation",
                    ReportTestScreenshots.SECOND_PIXEL_PNG, List.of(),
                    List.of(new AtfUiActionRecord(2, "Get text", "#confirmation",
                            "read result", "confirmed", 3, AtfReportStatus.PASS)),
                    ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
            finalBody.complete(AtfReportStatus.PASS);
            finalScenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(1, count(html, "<td>EVIDENCE TYPE</td><td>unified-ui-flow</td>")),
                () -> assertTrue(html.contains("<td>FLOW SCENARIO</td><td>final-only-second</td>")),
                () -> assertTrue(html.contains("<span>THEN terminal state appears</span>")),
                () -> assertFalse(html.contains("GIVEN discarded initial state")),
                () -> assertTrue(html.contains("<span>GIVEN checkpoint state is ready</span>")),
                () -> assertTrue(html.contains("<span>THEN checkpoint result appears</span>")),
                () -> assertEquals(3, count(html, "class=\"atf-ui-action-table\"")),
                () -> assertEquals(2, inlineScreenshots(html)),
                () -> assertEquals(1, count(html, ReportTestScreenshots.ONE_PIXEL_PNG)),
                () -> assertEquals(1, count(html, ReportTestScreenshots.SECOND_PIXEL_PNG)));
    }

    private static ReportExecutionEvidence paymentEvidence() {
        var request = new ReportRequest("POST", "/checkout", Map.of(),
                Map.of("Content-Type", "application/json"), "none", "{\"items\":[1]}");
        var response = new ReportResponse(201, "{\"id\":\"order-1\"}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(8),
                ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_CHECKOUT));
        return new ReportExecutionEvidence(response, 1, Duration.ofMillis(8), List.of(),
                List.of(new ReportAttemptRecord(1, 201, Duration.ofMillis(8),
                        "SUCCESS", "created")), request, ReportTransportProvenance.ACCEPTANCE_MOCK);
    }

    @Test
    void screenshotPopulationIgnoresExecutableSelectorsAndPreservesRealMultiplicity() {
        String selector = "<script>const selector='a[data-atf-screenshot=\"base64\"]';</script>";
        String image = "<a data-atf-screenshot=\"base64\" href=\"data:image/png;base64,fixture\">frame</a>";
        assertEquals(0, inlineScreenshots(selector.repeat(3)));
        assertEquals(1, inlineScreenshots(selector + image));
        assertEquals(2, inlineScreenshots(selector.repeat(3) + image + image));
        assertEquals(0, inlineScreenshots(selector + image.replace("data-atf-screenshot", "data-removed-capture")));
    }

    private static int inlineScreenshots(String html) {
        return org.jsoup.Jsoup.parse(html).select("a[data-atf-screenshot=base64]").size();
    }

    private static int count(String html, String fragment) {
        int count = 0;
        int from = 0;
        while ((from = html.indexOf(fragment, from)) >= 0) {
            count++;
            from += fragment.length();
        }
        return count;
    }
}
