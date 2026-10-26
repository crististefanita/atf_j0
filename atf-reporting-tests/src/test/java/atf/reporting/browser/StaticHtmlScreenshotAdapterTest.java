package atf.reporting.browser;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.ApiReportTestUrls;
import atf.testkit.AtfIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the optional real-browser seam against a locally generated report only. */
@AtfIntegrationTest
class StaticHtmlScreenshotAdapterTest {
    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 900;

    @Test
    void capturesGeneratedStaticReportAsRealPng() throws Exception {
        Path root = Files.createTempDirectory("atf-static-report-browser-");
        Path report = root.resolve("report.html");
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("StaticReportContract", "render browser proof",
                    List.of("reporting", "browser"));
            var phase = scenario.startPhase("TEST BODY");
            phase.startRequest("GET /static-report", "GET", ApiReportTestUrls.example("/static-report"))
                    .complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        Path screenshot = root.resolve("proof.png");
        var capture = new StaticHtmlScreenshotAdapter().capture(
                report, screenshot, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        assertEquals(VIEWPORT_WIDTH, capture.viewportWidth());
        assertEquals(VIEWPORT_HEIGHT, capture.viewportHeight());
        assertTrue(capture.png(), "static report screenshot must have a PNG signature");
        assertTrue(Files.size(capture.output()) > 0, "screenshot must not be empty");
    }
}
