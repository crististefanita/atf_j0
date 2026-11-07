package atf.reporting.ui;
import static atf.reporting.support.screenshot.ScreenshotLinkContract.assertSingleOriginal;
import atf.testkit.AtfUnitTest;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.evidence.ExtentReportUiEvidence;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.ReportTestScreenshots;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static atf.reporting.ui.support.ExtentReportUiEvidenceTestSupport.count;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportUiScreenshotContractTest {
    @Test
    void rendersMediaModeAsActionTablePlusBase64ImageLink() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-media-validation-");
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "ui-media-validation.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", "atf.ui.tests.extent", "media validation", List.of("ui"));
            var body = scenario.startPhase("TEST BODY");
            body.uiValidationAsMedia("category filter is visible", "#category-view",
                    ReportTestScreenshots.ONE_PIXEL_PNG, List.of(new AtfAssertion(
                            "Category title", "visible", "visible", true)),
                    List.of(new AtfUiActionRecord(1, "Click", "category=contract",
                            "detail opens", "click completed", 8, AtfReportStatus.PASS)),
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\"")
                        && html.contains(">📷 base64 img</span>")),
                () -> assertTrue(html.contains(".atf-ui-action-screenshot{display:flex!important;"
                        + "justify-content:flex-start!important;align-items:center!important;text-align:left!important;"
                        + "vertical-align:middle!important;width:100%!important;max-width:100%!important;")
                        && html.contains("padding:0!important")
                        && html.contains("overflow:visible!important;box-sizing:border-box!important")),
                () -> assertTrue(html.contains(".atf-ui-action-screenshot-link{display:flex!important;"
                        + "flex-direction:row!important;align-items:center!important;align-self:center!important;"
                        + "justify-content:flex-start!important;flex-wrap:wrap!important;gap:4px!important;"
                        + "width:100%!important;max-width:100%!important;height:auto!important;min-height:18px!important;")),
                () -> assertTrue(html.contains("text-decoration:none!important")),
                () -> assertTrue(html.contains("class=\"atf-ui-action-sequence\"")),
                () -> assertTrue(html.contains("href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\"")),
                () -> assertTrue(html.contains("data-featherlight=\"image\"")),
                () -> assertFalse(html.contains("atf-ui-action-screenshot-image")));
    }

    @Test
    void collapsesRepeatedScreenshotPayloadsBeforePublishingTheActionTable() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-repeated-screenshot-");
        Path report = root.resolve("ui-repeated-screenshot.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", "atf.ui.tests.extent", "repeated visual state", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN the same visual state is reused", "", "");
            step.uiActions(List.of(new AtfUiActionRecord(1, "Click", "#tests",
                    "tests opens", "click completed", 4, AtfReportStatus.PASS)));
            step.screenshot("first visual state", ReportTestScreenshots.ONE_PIXEL_PNG);
            step.screenshot("same visual state again", " " + ReportTestScreenshots.ONE_PIXEL_PNG + " ");
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertSingleOriginal(
                        html, ReportTestScreenshots.ONE_PIXEL_PNG, "first visual state"),
                () -> assertEquals(1, count(html, "data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG)),
                () -> assertTrue(html.contains("aria-label=\"Open first visual state\"")),
                () -> assertFalse(html.contains("same visual state again")));
        var document = org.jsoup.Jsoup.parse(html);
        var link = document.selectFirst("a[data-atf-screenshot=base64]");
        link.after(link.outerHtml());
        assertThrows(AssertionError.class, () -> assertSingleOriginal(
                document.outerHtml(), ReportTestScreenshots.ONE_PIXEL_PNG, "first visual state"));
        link.nextElementSibling().remove();
        link.attr("href", "data:image/png;base64," + ReportTestScreenshots.SECOND_PIXEL_PNG);
        assertThrows(AssertionError.class, () -> assertSingleOriginal(
                document.outerHtml(), ReportTestScreenshots.ONE_PIXEL_PNG, "first visual state"));
        link.attr("href", "data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG).attr("aria-label", "Open another checkpoint");
        assertThrows(AssertionError.class, () -> assertSingleOriginal(
                document.outerHtml(), ReportTestScreenshots.ONE_PIXEL_PNG, "first visual state"));
    }

    @Test
    void collapsesRepeatedOverviewCaptureBytesWithoutRenderingInlineImages() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-overview-dedup-");
        Path report = root.resolve("report.html");
        Path first = root.resolve("first.png");
        Path duplicate = root.resolve("duplicate.png");
        Files.writeString(report, "<body><div id=\"dashboard-view\">overview</div></body>");
        Files.write(first, Base64.getDecoder().decode(ReportTestScreenshots.ONE_PIXEL_PNG));
        Files.write(duplicate, Base64.getDecoder().decode(ReportTestScreenshots.ONE_PIXEL_PNG));

        ExtentReportUiEvidence.embedInlinePngs(report, List.of(
                new ExtentReportUiEvidence.Capture(first, "first capture", List.of("GIVEN first")),
                new ExtentReportUiEvidence.Capture(duplicate, "duplicate capture", List.of("GIVEN duplicate"))));

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Playwright exploratory evidence · 1 captures")),
                () -> assertEquals(1, count(html, "data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG)),
                () -> assertTrue(html.contains("first capture")),
                () -> assertFalse(html.contains("duplicate capture")),
                () -> assertFalse(html.contains("src=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\"")));
    }

    @Test
    void allowsUiActionTableWhenScreenshotPolicyOmitsCheckpointImage() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-action-screenshot-contract-");
        Path report = root.resolve("ui-actions-without-screenshot.html");

        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", "atf.ui.tests.extent", "invalid visual audit", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN the page is visible", "", "");
            step.uiActions(List.of(new AtfUiActionRecord(1, "Click", "#submit",
                    "page changes", "click completed", 12, AtfReportStatus.PASS)));

            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("atf-ui-action-sequence"),
                        "Playwright actions remain reportable when the configured policy omits screenshots"),
                () -> assertFalse(html.contains("class=\"atf-ui-action-screenshot-link\""),
                        "an omitted screenshot policy must not synthesize image evidence"));
    }

    @Test
    void rejectsUiActionTableWithMoreThanOneBase64Screenshot() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-action-multiple-screenshots-");
        Path report = root.resolve("ui-actions-with-multiple-screenshots.html");

        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiTest", "atf.ui.tests.extent", "invalid visual audit", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startRequest("THEN the page is visible", "", "");
            step.uiActions(List.of(new AtfUiActionRecord(1, "Click", "#submit",
                    "page changes", "click completed", 12, AtfReportStatus.PASS)));
            step.screenshot("first state", ReportTestScreenshots.ONE_PIXEL_PNG);
            assertThrows(IllegalStateException.class,
                    () -> step.screenshot("second state", ReportTestScreenshots.SECOND_PIXEL_PNG),
                    "an action table must have exactly one local screenshot");
            step.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }
}


