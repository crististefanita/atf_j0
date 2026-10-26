package atf.reporting.ui.profile;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cross-phase UI-only table contract at the Reporting boundary, independent of Shop. */
@AtfComponentTest
class UiCrossPhaseCompactFlowComponentTest {
    private static final String TERMINAL_IMAGE = ReportTestScreenshots.SECOND_PIXEL_PNG;

    @Test
    void successfulUiOnlyFlowHasOneOrderedSetOfEvidenceInBothRenderers() throws Exception {
        for (boolean extent : List.of(true, false)) {
            String html = render(extent, ReportScreenshotPolicy.FINAL_ONLY, false);
            assertEquals(1, count(html, "class=\"atf-ui-action-sequence\""));
            assertEquals(1, count(html, "class=\"atf-ui-observations\""));
            assertEquals(1, count(html, "atf-ui-observation-table\""));
            assertEquals(1, count(html, extent ? "class=\"atf-ui-assertions\"" : "class=\"assertions\""));
            assertEquals(1, count(html, "data-atf-screenshot=\"base64\""));
            assertEquals(1, count(html, "atf-ui-flow-provenance\""));
            assertFalse(html.contains(">Response details ·"));
            assertFalse(html.contains("SETUP · Setup validation"));
            assertFalse(html.contains("TEST BODY · UI validation"));
            assertTrue(html.contains("UI flow provenance"));
            assertTrue(html.contains("setup assertion"));
            assertTrue(html.contains("body assertion"));
            assertTrue(html.contains("cleanup assertion"));
            assertTrue(html.indexOf("setup action") < html.indexOf("body action"));
            assertTrue(html.indexOf("body action") < html.indexOf("cleanup action"));
            assertEquals(2, count(html, "atf-ui-action-operation\">body action</span>"));
            int actionTable = html.indexOf("atf-ui-action-table-wrap", html.indexOf("setup action") - 1000);
            int screenshot = html.indexOf("data-atf-screenshot=\"base64\"", actionTable);
            int observation = html.indexOf("atf-ui-observations", screenshot);
            assertTrue(actionTable >= 0 && screenshot > actionTable && observation > screenshot);
            assertTrue(html.contains("data:image/png;base64," + TERMINAL_IMAGE));
        }
    }

    @Test
    void capturePolicyNoneKeepsTablesWithoutInventingAnImage() throws Exception {
        for (boolean extent : List.of(true, false)) {
            String html = render(extent, ReportScreenshotPolicy.NONE, false);
            assertEquals(1, count(html, "class=\"atf-ui-action-sequence\""));
            assertEquals(0, count(html, "data-atf-screenshot=\"base64\""));
            assertTrue(html.contains("cleanup assertion"));
            assertFalse(html.contains("setup action"));
        }
    }

    @Test
    void failedSetupRetainsItsVerdictAndDoesNotInventBody() throws Exception {
        for (boolean extent : List.of(true, false)) {
            String html = render(extent, ReportScreenshotPolicy.FINAL_ONLY, true);
            assertTrue(html.contains("SETUP · Setup validation"));
            assertTrue(html.contains("rejected"));
            assertFalse(html.contains("body action"));
            assertEquals(0, count(html, "data-atf-screenshot=\"base64\""));
        }
    }

    @Test
    void absentAuditAndAssertionsDoNotProduceEmptyTables() throws Exception {
        for (boolean extent : List.of(true, false)) {
            Path report = Files.createTempDirectory("atf-compact-empty-").resolve("report.html");
            try (AtfReportPorts.Session session = extent
                    ? new ExtentReportSession(report) : new HtmlReportSession(report)) {
                var scenario = session.startScenario("CompactFlowTest", "atf.tests", "empty families", List.of());
                var phase = scenario.startPhase("TEST BODY");
                var flow = phase.startRequest("THEN action completes", "", "");
                flow.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.UNIFIED_UI_FLOW);
                flow.detail(AtfReportEvidenceKeys.FLOW_PRESENTATION, AtfReportEvidenceKeys.COMPACT_UI_ONLY);
                var validation = flow.startValidation("UI validation");
                validation.uiActions(List.of(action(1, "only action", AtfUiEvidenceSource.ACTION)));
                validation.assertions(List.of());
                validation.complete(AtfReportStatus.PASS);
                flow.complete(AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
            String html = Files.readString(report);
            assertEquals(1, count(html, "class=\"atf-ui-action-sequence\""));
            assertEquals(0, count(html, "class=\"atf-ui-observations\""));
            assertEquals(0, count(html, extent
                    ? "class=\"atf-ui-assertions\"" : "class=\"assertions\""));
        }
    }

    private static String render(boolean extent, ReportScreenshotPolicy policy, boolean failSetup)
            throws Exception {
        Path directory = Path.of(System.getProperty("atf.test.reportDirectory",
                "artifacts/reports/ui"));
        Files.createDirectories(directory);
        Path report = directory.resolve("unified-cross-phase-" + (extent ? "extent" : "fallback")
                + "-" + policy.configValue() + "-" + failSetup + "-" + System.nanoTime() + ".html");
        ReportEvidenceProfile profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW).screenshotPolicy(policy).build();
        if (extent) {
            try (UiExtentReportSession session = UiExtentReportSession.openConfigured(
                    directory, report.getFileName().toString(), AtfConsoleSink.noOp(),
                    profile)) {
                report = session.reportPath();
                var scenario = session.startScenario("CompactFlowTest", "atf.tests", "single UI flow", List.of());
                scenario.unifyAcrossPhases();
                var setup = scenario.startPhase("SETUP");
                setup.setup(AtfGherkinKeyword.GIVEN, "account is ready", "/login",
                        List.of(new AtfAssertion("setup assertion", "ready",
                                failSetup ? "rejected" : "ready", !failSetup)),
                        policy == ReportScreenshotPolicy.NONE ? List.of()
                                : List.of(action(1, "setup action", AtfUiEvidenceSource.ACTION)),
                        UiApiValidationEvidence.empty(),
                        failSetup ? AtfReportStatus.FAIL : AtfReportStatus.PASS);
                if (failSetup) {
                    setup.completeFinal(AtfReportStatus.FAIL);
                    scenario.complete(AtfReportStatus.FAIL);
                } else {
                    setup.complete(AtfReportStatus.PASS);
                    var body = scenario.startPhase("TEST BODY");
                    body.step(AtfGherkinKeyword.WHEN, "purchase is submitted", "#confirm", null,
                            List.of(new AtfAssertion("body assertion", "submitted", "submitted", true)),
                            List.of(action(2, "body action", AtfUiEvidenceSource.ACTION),
                                    action(3, "body action", AtfUiEvidenceSource.ACTION),
                                    action(4, "body observation", AtfUiEvidenceSource.OBSERVE)),
                            AtfReportStatus.PASS);
                    body.screenValidation("purchase is confirmed", "#receipt", TERMINAL_IMAGE,
                            List.of(new AtfAssertion("terminal assertion", "confirmed", "confirmed", true)),
                            List.of(action(5, "confirmation action", AtfUiEvidenceSource.ACTION)),
                            ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
                    body.complete(AtfReportStatus.PASS);
                    var cleanup = scenario.startPhase("CLEANUP");
                    cleanup.step(AtfGherkinKeyword.AND, "session is closed", "#logout", null,
                            List.of(new AtfAssertion("cleanup assertion", "closed", "closed", true)),
                            List.of(action(6, "cleanup action", AtfUiEvidenceSource.ACTION)),
                            AtfReportStatus.PASS);
                    cleanup.completeFinal(AtfReportStatus.PASS);
                    scenario.complete(AtfReportStatus.PASS);
                }
            }
        } else {
            renderFallback(report, profile, policy, failSetup);
        }
        System.out.println("[ATF] Compact UI flow report: " + report.toAbsolutePath());
        return Files.readString(report);
    }

    private static void renderFallback(Path report, ReportEvidenceProfile profile,
                                       ReportScreenshotPolicy policy, boolean failSetup) {
        try (HtmlReportSession session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("CompactFlowTest", "atf.tests", "single UI flow", List.of());
            var setup = scenario.startPhase("SETUP");
            if (failSetup) {
                var flow = setup.startRequest("GIVEN account is ready", "", "");
                flow.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.UNIFIED_UI_FLOW);
                var validation = flow.startValidation("SETUP · Setup validation");
                validation.uiActions(List.of(action(1, "setup action", AtfUiEvidenceSource.ACTION)));
                validation.assertions(List.of(new AtfAssertion("setup assertion", "ready", "rejected", false)));
                validation.complete(AtfReportStatus.FAIL);
                flow.complete(AtfReportStatus.FAIL);
                setup.complete(AtfReportStatus.FAIL);
                scenario.complete(AtfReportStatus.FAIL);
                return;
            }
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase("TEST BODY");
            var flow = body.startRequest("THEN purchase is confirmed", "", "");
            flow.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.UNIFIED_UI_FLOW);
            flow.detail(AtfReportEvidenceKeys.FLOW_MODE, profile.flowMode().configValue());
            flow.detail(AtfReportEvidenceKeys.FLOW_PRESENTATION, AtfReportEvidenceKeys.COMPACT_UI_ONLY);
            flow.detail(AtfReportEvidenceKeys.FLOW_SCENARIO, "single UI flow");
            flow.detail(AtfReportEvidenceKeys.FLOW_PHASE, "SETUP → TEST BODY → CLEANUP");
            for (int index = 1; index <= 4; index++) {
                flow.detail(AtfReportEvidenceKeys.flowCheckpoint(index) + ".phase",
                        index == 1 ? "SETUP" : index == 4 ? "CLEANUP" : "TEST BODY");
                flow.detail(AtfReportEvidenceKeys.flowCheckpoint(index) + ".phaseStatus", "pass");
            }
            var validation = flow.startValidation("UI validation");
            var actions = new ArrayList<AtfUiActionRecord>();
            if (policy != ReportScreenshotPolicy.NONE) {
                actions.add(action(1, "setup action", AtfUiEvidenceSource.ACTION));
            }
            actions.add(action(2, "body action", AtfUiEvidenceSource.ACTION));
            actions.add(action(3, "body action", AtfUiEvidenceSource.ACTION));
            actions.add(action(4, "body observation", AtfUiEvidenceSource.OBSERVE));
            actions.add(action(5, "confirmation action", AtfUiEvidenceSource.ACTION));
            actions.add(action(6, "cleanup action", AtfUiEvidenceSource.ACTION));
            validation.uiActions(actions);
            if (policy.captures(ReportScreenshotPolicy.CapturePoint.FINAL)) {
                validation.screenshot("UI screenshot — purchase is confirmed", TERMINAL_IMAGE);
            }
            validation.assertions(List.of(
                    new AtfAssertion("setup assertion", "ready", "ready", true),
                    new AtfAssertion("body assertion", "submitted", "submitted", true),
                    new AtfAssertion("terminal assertion", "confirmed", "confirmed", true),
                    new AtfAssertion("cleanup assertion", "closed", "closed", true)));
            validation.complete(AtfReportStatus.PASS);
            flow.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.startPhase("CLEANUP").complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }

    private static AtfUiActionRecord action(int sequence, String name, AtfUiEvidenceSource source) {
        return new AtfUiActionRecord(sequence, name, "#target", "expected", "observed", 1,
                AtfReportStatus.PASS, source);
    }
}
