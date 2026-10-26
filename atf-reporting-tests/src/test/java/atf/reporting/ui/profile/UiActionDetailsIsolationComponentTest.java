package atf.reporting.ui.profile;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real publisher/renderer boundaries; no remote browser or string-only mock of a report. */
@AtfComponentTest
class UiActionDetailsIsolationComponentTest {
    @TempDir Path output;

    @Test
    void neighboringValidationPoliciesStayLocalInBothRenderersAndRetainScreenshots() throws Exception {
        for (boolean extent : List.of(true, false)) {
            Path path = output.resolve("scoped-" + extent + ".html");
            try (AtfReportPorts.Session report = extent ? new ExtentReportSession(path) : new HtmlReportSession(path)) {
                var scenario = report.startScenario("DetailsScope", "different disclosure policies", List.of());
                var phase = scenario.startPhase("TEST BODY");
                for (var mode : ReportUiActionDetailsMode.values()) {
                    var request = phase.startRequest(mode.name(), "", "");
                    var validation = request.startValidation("UI validation");
                    validation.uiActionDetailsMode(mode);
                    validation.uiActions(actions(mode == ReportUiActionDetailsMode.SUMMARY_ONLY ? 1 : 2));
                    validation.screenshot("observed state " + mode, mode == ReportUiActionDetailsMode.SUMMARY_ONLY
                            ? ReportTestScreenshots.ONE_PIXEL_PNG : ReportTestScreenshots.SECOND_PIXEL_PNG);
                    validation.assertions(List.of(new AtfAssertion("Order", "confirmed", "confirmed", true)));
                    validation.complete(AtfReportStatus.PASS);
                    validation.close();
                    request.complete(AtfReportStatus.PASS);
                    request.close();
                }
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
            assertDisclosureIsolation(Files.readString(path));
        }
    }

    @Test
    void explicitScenarioProfilesReachCheckpointAndUnifiedPublishersInEitherOrder() throws Exception {
        for (boolean summaryFirst : List.of(true, false)) {
            Path path;
            try (var report = UiExtentReportSession.openConfigured(output, "profiles-" + summaryFirst + ".html",
                    AtfConsoleSink.noOp(), ReportEvidenceProfile.defaults())) {
                path = report.reportPath();
                for (boolean summary : List.of(summaryFirst, !summaryFirst)) {
                    var profile = ReportEvidenceProfile.builder()
                            .flowMode(summary ? ReportEvidenceFlowMode.SCREEN_CHECKPOINTS : ReportEvidenceFlowMode.UNIFIED_FLOW)
                            .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                            .actionDetailsMode(summary ? ReportUiActionDetailsMode.SUMMARY_ONLY
                                    : ReportUiActionDetailsMode.INCLUDED_COLLAPSED).build();
                    var scenario = report.startScenario("Profile" + summary, "atf.tests", "scoped actions", List.of(), profile);
                    var body = scenario.startPhase("TEST BODY");
                    body.uiValidation("the observed order is confirmed", "/checkout",
                            summary ? ReportTestScreenshots.ONE_PIXEL_PNG : ReportTestScreenshots.SECOND_PIXEL_PNG,
                            List.of(new AtfAssertion("Order", "confirmed", "confirmed", true)), actions(summary ? 1 : 2),
                            ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
                    body.completeFinal(AtfReportStatus.PASS);
                    scenario.complete(AtfReportStatus.PASS);
                }
            }
            assertDisclosureIsolation(Files.readString(path));
        }
    }

    @Test
    void setupAndFailureUseTheScenarioPolicyWithoutDroppingTheRecordedActions() throws Exception {
        var profile = ReportEvidenceProfile.builder().actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY).build();
        Path path;
        try (var report = UiExtentReportSession.openConfigured(output, "summary-failure.html", AtfConsoleSink.noOp(),
                ReportEvidenceProfile.defaults())) {
            path = report.reportPath();
            var scenario = report.startScenario("SummaryFailure", "atf.tests", "failure stays auditable", List.of(), profile);
            var setup = scenario.startPhase("SETUP");
            setup.setup(AtfGherkinKeyword.GIVEN, "customer is ready", "/login", List.of(), actions(1),
                    UiApiValidationEvidence.empty(), AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase("TEST BODY");
            body.failureEvidence("checkout fails", "/checkout", actions(2),
                    new IllegalStateException("observed rejection"), ReportTestScreenshots.ONE_PIXEL_PNG);
            body.completeFinal(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            assertEquals(AtfReportStatus.FAIL, report.semanticSnapshot().classes().getFirst()
                    .scenarios().getFirst().status());
        }
        String html = visibleMarkup(Files.readString(path));
        assertEquals(2, actionDisclosures(html).size());
        assertFalse(html.contains("class=\"atf-ui-action-table\""));
        assertEquals(1, Pattern.compile("data-atf-screenshot=\"base64\"").matcher(html).results().count());
        assertTrue(html.contains("observed rejection"));
    }

    private static void assertDisclosureIsolation(String source) {
        var disclosures = actionDisclosures(source);
        assertEquals(2, disclosures.size());
        var summary = disclosures.stream().filter(node -> node.replaceAll("<[^>]+>", "").contains(AtfReportLabels.actionSequence(1)))
                .findFirst().orElseThrow();
        var detailed = disclosures.stream().filter(node -> node.replaceAll("<[^>]+>", "").contains(AtfReportLabels.actionSequence(2)))
                .findFirst().orElseThrow();
        assertFalse(summary.contains("<table"));
        assertTrue(detailed.contains("class=\"atf-ui-action-table\""));
        assertTrue(summary.contains("data-atf-screenshot=\"base64\""));
        assertTrue(detailed.contains("data-atf-screenshot=\"base64\""));
        assertFalse(summary.substring(0, summary.indexOf('>')).contains(" open"));
        assertFalse(detailed.substring(0, detailed.indexOf('>')).contains(" open"));
    }

    private static List<String> actionDisclosures(String source) {
        return Pattern.compile("(?s)<details\\b[^>]*class=\"atf-ui-action-sequence\"[^>]*>.*?</details>")
                .matcher(visibleMarkup(source)).results().map(match -> match.group()).toList();
    }

    private static String visibleMarkup(String source) {
        return source.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
    }

    private static List<AtfUiActionRecord> actions(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(index -> new AtfUiActionRecord(index,
                "Get text", "#order-" + index, "read confirmed order", "confirmed", 1, AtfReportStatus.PASS)).toList();
    }
}
