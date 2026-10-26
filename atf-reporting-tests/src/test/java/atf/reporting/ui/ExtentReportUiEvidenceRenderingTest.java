package atf.reporting.ui;
import atf.testkit.AtfUnitTest;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.evidence.ExtentReportUiEvidence;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.ReportTestScreenshots;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static atf.reporting.ui.support.ExtentReportUiEvidenceTestSupport.EXTENT_UI_TEST_PACKAGE;
import static atf.reporting.ui.support.ExtentReportUiEvidenceTestSupport.count;
import static atf.reporting.ui.support.ExtentReportUiEvidenceTestSupport.screenshotAnchorContainsDirectDataUri;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportUiEvidenceRenderingTest {
    @Test
    void embedsPlaywrightPngAndAuditStepsInsideOverview() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-evidence-");
        Path report = root.resolve("report.html");
        Path png = root.resolve("capture.png");
        Files.writeString(report, "<body><div id=\"dashboard-view\">overview</div></body>");
        Files.write(png, Base64.getDecoder().decode(ReportTestScreenshots.ONE_PIXEL_PNG));

        ExtentReportUiEvidence.embedInlinePngs(report, List.of(new ExtentReportUiEvidence.Capture(
                png, "Tests tab", List.of("GIVEN report is open", "THEN class owners are visible"))));

        String html = Files.readString(report);
        assertTrue(html.contains("id=\"atf-ui-exploratory-evidence\""));
        assertTrue(html.contains("href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\""));
        assertTrue(html.contains("class=\"atf-ui-evidence-screenshot-link\""));
        assertTrue(html.contains("align-items:center;align-self:center;justify-content:center;gap:4px;height:18px;min-height:18px;"));
        assertTrue(html.contains("vertical-align:middle;margin:8px 0 0;transform:translateY(2px);"));
        assertTrue(html.contains("target=\"_blank\""));
        assertTrue(html.contains(">📷 base64 img</span>"));
        assertTrue(html.contains("aria-label=\"Open Tests tab\""));
        assertTrue(html.contains("data-featherlight=\"image\""));
        assertTrue(html.contains("rel=\"noopener\""));
        assertFalse(html.contains("atf-ui-action-screenshot-image"));
        assertTrue(html.contains("GIVEN report is open"));
        assertTrue(html.contains("<details>"));
        assertFalse(html.contains("<details open>"));
        assertFalse(html.contains("capture.png"));
    }

    @Test
    void rendersUiScreenshotAsBase64LinkEvidenceOnTheUiStep() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-report-");
        Path report = root.resolve("ui-report.html");

        try (ExtentReportSession session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE, "report inspection", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN report is inspectable", "", "");
            step.screenshot("Playwright screenshot", ReportTestScreenshots.ONE_PIXEL_PNG);
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertTrue(html.contains(">📷 base64 img</span>"));
        assertTrue(html.contains("aria-label=\"Open Playwright screenshot\""));
        assertTrue(html.contains("data-featherlight=\"image\""));
        assertFalse(html.contains("| Playwright screenshot"));
        assertTrue(html.contains("href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\""));
        assertTrue(html.contains("target=\"_blank\""));
        assertTrue(html.contains("rel=\"noopener\""));
        assertFalse(html.contains("atf-ui-action-screenshot-image"));
        assertTrue(html.contains("Playwright screenshot"));
    }

    @Test
    void rendersConsecutivePlaywrightActionsAsOneCollapsibleTableWithEmojiResult() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-action-sequence-");
        Path report = root.resolve("ui-actions.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE, "action sequence", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN checklist is open", "", "");
            step.uiActions(List.of(
                    new AtfUiActionRecord(1, "Click", "role=tab[name=Checklist]",
                            "checklist view opens", "click completed", 549, AtfReportStatus.PASS),
                    new AtfUiActionRecord(27, "Get text", "role=heading[name=Quote Checklist]",
                            "heading is visible", "Quote Checklist", 12, AtfReportStatus.PASS)));
            step.screenshot("UI validation", ReportTestScreenshots.ONE_PIXEL_PNG);
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("class=\"atf-ui-action-sequence\"")),
                () -> assertTrue(html.contains("Playwright action sequence (2 entries)")),
                () -> assertFalse(html.contains("<details class=\"atf-ui-action-sequence\" open")),
                () -> assertTrue(html.contains("<th>ACTION</th>") && html.contains("<th>TARGET</th>")),
                () -> assertTrue(html.contains("class=\"atf-ui-action-identity\"><span class=\"atf-ui-action-operation\">Get text</span>")),
                () -> assertTrue(html.contains("class=\"atf-ui-action-number\">#1</span>")),
                () -> assertTrue(html.contains("class=\"atf-ui-action-number\">#27</span>")),
                () -> assertFalse(html.contains("class=\"atf-ui-action-sequence\">#1</span>")),
                () -> assertFalse(html.contains("class=\"atf-ui-action-sequence\">#27</span>")),
                () -> assertTrue(html.contains("Get text")),
                () -> assertTrue(html.contains("role=heading[name=Quote Checklist]")),
                () -> assertTrue(html.contains("class=\"atf-ui-action-time\">549 ms</span>")),
                () -> assertTrue(html.contains("atf-ui-action-result-pass")),
                () -> assertTrue(html.contains("role=\"img\" aria-label=\"Pass\" title=\"Pass\">✅</span>")),
                () -> assertFalse(html.contains("class=\"atf-ui-action-result atf-ui-action-result-pass\">Pass</span>")),
                () -> assertTrue(html.contains("class=\"atf-ui-action-screenshot\"")),
                () -> assertTrue(html.contains("</table></div><div class=\"atf-ui-action-screenshot\""),
                        "the screenshot must remain outside the scrollable action-table viewport"),
                () -> assertTrue(html.contains("class=\"atf-ui-action-screenshot-link\" data-atf-screenshot=\"base64\" href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\"")),
                () -> assertTrue(html.contains(">📷 base64 img</span>")),
                () -> assertTrue(html.contains("aria-label=\"Open UI validation\"")),
                () -> assertTrue(html.contains("data-featherlight=\"image\"")),
                () -> assertTrue(html.contains("target=\"_blank\"") && html.contains("rel=\"noopener\"")),
                () -> assertFalse(html.contains("atf-ui-action-screenshot-image")),
                () -> assertFalse(html.contains(">passed</span>")));
    }

    @Test
    void rendersReplayValueBesideTheFillTargetAndObservedResult() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-replay-value-");
        Path report = root.resolve("ui-replay-value.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "replay value evidence", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("WHEN billing postal code is entered", "", "");
            step.uiActions(List.of(new AtfUiActionRecord(12, "Fill",
                    "input[data-test='postal_code']",
                    "billing postal code is entered · value=010101",
                    "value entered: 010101", 5, AtfReportStatus.PASS)));
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertTrue(html.contains("input[data-test='postal_code']"));
        assertTrue(html.contains("billing postal code is entered · value=010101"));
        assertTrue(html.contains("value entered: 010101"));
    }

    @Test
    void rendersSensitivePlaywrightEvidenceWithoutPublishingTheOriginalValues() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-sensitive-evidence-");
        Path report = root.resolve("ui-sensitive-evidence.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "sensitive payment evidence", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("WHEN payment credentials are entered", "", "");
            step.uiActions(List.of(new AtfUiActionRecord(1, "Fill",
                    "input[id='card-number']", "payment card is entered · value=[REDACTED]",
                    "value entered: [REDACTED]", 5, AtfReportStatus.PASS),
                    new AtfUiActionRecord(2, "Fill", "input[id='cvc']",
                            "security code is entered · value=[REDACTED]",
                            "value entered: [REDACTED]", 2, AtfReportStatus.PASS)));
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertTrue(html.contains("Playwright action sequence (2 entries)"));
        assertTrue(html.contains("value entered: [REDACTED]"));
        assertFalse(html.contains("4111111111111111"));
        assertFalse(html.contains("987"));
    }

    @Test
    void summaryOnlyKeepsOneCollapsedDisclosureAndOmitsTheActionTable() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-action-summary-only-");
        var profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                .captureSource(ReportEvidenceCaptureSource.CUSTOM_PROVIDER)
                .actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY)
                .build();

        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "summary-only.html",
                AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "summary-only action disclosure", List.of("ui"));
            var body = scenario.startPhase("TEST BODY");
            body.uiValidation("the final state is visible", "#test-view",
                    ReportTestScreenshots.ONE_PIXEL_PNG, List.of(), List.of(
                            new AtfUiActionRecord(1, "Click", "#tests",
                                    "Tests opens", "click completed", 4, AtfReportStatus.PASS)),
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertTrue(html.contains("Playwright action sequence (1 entry)"));
        assertTrue(html.contains("class=\"atf-ui-action-sequence\""));
        assertFalse(html.contains("<th>ACTION</th>"));
        assertFalse(html.contains("#tests"));
        assertTrue(html.contains("data-atf-screenshot=\"base64\""));
        assertTrue(html.contains("</summary><div class=\"atf-ui-action-screenshot\""));
        assertTrue(html.contains("</a></div></details>"));
    }

    @Test
    void rendersAConfiguredReadOnlyRetryAsAnInlineInfoAction() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-retry-evidence-");
        Path report = root.resolve("ui-retry-evidence.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "bounded retry evidence", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN report stabilization completes", "", "");
            step.uiActions(List.of(
                    new AtfUiActionRecord(1, "Retry", "#test-view", "attempt 2 of 3",
                            "retry scheduled: DOM stabilization", 0, AtfReportStatus.INFO),
                    new AtfUiActionRecord(2, "Wait for visible", "#test-view", "state=Visible",
                            "state reached", 7, AtfReportStatus.PASS)));
            step.screenshot("UI validation", ReportTestScreenshots.ONE_PIXEL_PNG);
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Playwright action sequence (2 entries)")),
                () -> assertTrue(html.contains("Retry")),
                () -> assertTrue(html.contains("attempt 2 of 3")),
                () -> assertTrue(html.contains("retry scheduled: DOM stabilization")),
                () -> assertTrue(html.contains("role=\"img\" aria-label=\"Info\"")),
                () -> assertTrue(html.contains("Wait for visible")));
    }

    @Test
    void rendersOneScreenshotReferenceAndOneResultEmojiPerActionWithoutDuplication() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-deduplicated-evidence-");
        Path report = root.resolve("ui-deduplicated-evidence.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE, "deduplicated evidence", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN the page is auditable", "", "");
            step.uiActions(List.of(
                    new AtfUiActionRecord(1, "Click", "#tests", "Tests opens", "click completed",
                            4, AtfReportStatus.PASS),
                    new AtfUiActionRecord(2, "Get text", "h1", "heading is visible", "Tests",
                            2, AtfReportStatus.PASS)));
            step.screenshot("One visual state", ReportTestScreenshots.ONE_PIXEL_PNG);
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(count(html, "data-atf-screenshot=\"base64\"") == 1,
                        "one visual state must produce one screenshot reference"),
                () -> assertTrue(count(html, ">📷 base64 img</span>") == 1,
                        "the screenshot label must not be duplicated"),
                () -> assertTrue(count(html, "class=\"atf-ui-action-result atf-ui-action-result-pass\"") == 2,
                        "each consecutive action must have exactly one result emoji"),
                () -> assertTrue(html.contains("data-featherlight=\"image\"")),
                () -> assertFalse(html.contains("atf-ui-action-screenshot-image")),
                () -> assertTrue(count(html, "target=\"_blank\"") == 1),
                () -> assertTrue(count(html, "rel=\"noopener\"") == 1),
                () -> assertTrue(screenshotAnchorContainsDirectDataUri(html),
                        "the screenshot anchor must carry the one Base64 payload"),
                () -> assertTrue(count(html, "atf-ui-action-screenshot-image") == 0,
                        "one visual state must not render a duplicate image"),
                () -> assertTrue(count(html, "data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG) == 1,
                        "one visual state must contain one Base64 payload"));
    }

    @Test
    void rendersReporterObservationsSeparatelyFromPlaywrightActions() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-observations-");
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "ui-observations.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE, "observation split", List.of("ui"));
            var body = scenario.startPhase("TEST BODY");
            body.uiValidation("report state", "#test-view", ReportTestScreenshots.ONE_PIXEL_PNG, List.of(
                    new AtfAssertion("report state", "ready", "ready", true)), List.of(
                    new AtfUiActionRecord(1, "Click", "#tests", "tests opens", "click completed", 4, AtfReportStatus.PASS),
                    new AtfUiActionRecord(2, "Audit screen contract", "purchase confirmation",
                            "all component assertions pass",
                            "status=PASS; assertions=1; screenshot=valid PNG; url=/confirmation",
                            0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)), AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Playwright action sequence (1 entry)")),
                () -> assertTrue(html.contains("Audit observations · 1 checkpoint")),
                () -> assertEquals(1, count(html, "Audit screen contract")),
                () -> assertTrue(html.contains("screenshot=valid PNG; url=/confirmation")),
                () -> assertTrue(html.contains("aria-label=\"Audit observations · 1 checkpoint\"")),
                () -> assertFalse(html.contains("Audit observations · 1 checkpoint · Screenshot")),
                () -> assertTrue(html.contains("atf-ui-observation-table")),
                () -> assertTrue(html.contains("class=\"atf-ui-assertions\"")),
                () -> assertTrue(html.contains("class=\"atf-validation-table-wrap\"")),
                () -> assertTrue(html.contains(
                        "class=\"atf-ui-action-table atf-ui-observation-table\"><thead><tr>"
                                + "<th>ACTION</th><th>TARGET</th><th>EXPECTED</th>"
                                + "<th>OBSERVED</th><th>RESULT</th>")),
                () -> assertFalse(html.contains(
                        "class=\"atf-ui-action-table atf-ui-observation-table\"><thead><tr>"
                                + "<th>ACTION</th><th>TARGET</th><th>EXPECTED</th>"
                                + "<th>OBSERVED</th><th>TIME</th>")));
    }
}


