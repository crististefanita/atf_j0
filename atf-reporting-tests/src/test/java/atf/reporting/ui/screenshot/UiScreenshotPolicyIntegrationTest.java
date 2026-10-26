package atf.reporting.ui.screenshot;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies policy-to-HTML cardinality without a browser or external endpoint. */
@AtfIntegrationTest
class UiScreenshotPolicyIntegrationTest {
    @Test
    void eachCheckpointRetainsCheckpointEvidence() throws Exception {
        String html = render(ReportScreenshotPolicy.EACH_CHECKPOINT, true, false, false);

        assertEquals(1, count(html, "data-atf-screenshot=\"base64\""));
    }

    @Test
    void finalOnlyPublishesOnlyAnExplicitFinalCapture() throws Exception {
        String html = render(ReportScreenshotPolicy.FINAL_ONLY, false, true, false);

        assertEquals(1, count(html, "data-atf-screenshot=\"base64\""));
    }

    @Test
    void failureOnlyPublishesOnlyFailureEvidence() throws Exception {
        String html = render(ReportScreenshotPolicy.FAILURE_ONLY, true, false, true);

        assertEquals(1, count(html, "data-atf-screenshot=\"base64\""));
    }

    @Test
    void finalAndFailurePublishesBothExplicitEvidencePoints() throws Exception {
        String html = render(ReportScreenshotPolicy.FINAL_AND_FAILURE, true, true, true);

        assertEquals(2, count(html, "data-atf-screenshot=\"base64\""));
    }

    @Test
    void nonePublishesNoScreenshotEvenWhenPayloadsAreAvailable() throws Exception {
        String html = render(ReportScreenshotPolicy.NONE, true, true, true);

        assertEquals(0, count(html, "data-atf-screenshot=\"base64\""));
    }

    @Test
    void aPublishedScreenshotRequiresAStableTarget() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-policy-target-");
        assertThrows(IllegalArgumentException.class, () -> {
            try (var session = UiExtentReportSession.openConfigured(root, "target.html",
                    AtfConsoleSink.noOp(), ReportScreenshotPolicy.EACH_CHECKPOINT)) {
                var scenario = session.startScenario("PolicyTest", "atf.reporting.ui",
                        "missing target", List.of("ui"));
                var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
                body.screenValidation("visual state", " ", ReportTestScreenshots.ONE_PIXEL_PNG,
                        List.of(), List.of(), AtfReportStatus.PASS);
            }
        });
    }

    private static String render(ReportScreenshotPolicy policy,
                                 boolean checkpoint,
                                 boolean finalState,
                                 boolean failure) throws Exception {
        Path root = Files.createTempDirectory("atf-ui-policy-");
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "policy.html",
                AtfConsoleSink.noOp(), policy)) {
            report = session.reportPath();
            var scenario = session.startScenario("PolicyTest", "atf.reporting.ui",
                    "screenshot policy", List.of("ui"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            if (checkpoint) {
                body.screenValidation("checkpoint", "#checkpoint", ReportTestScreenshots.ONE_PIXEL_PNG,
                        List.of(), List.of(), AtfReportStatus.PASS);
            }
            if (finalState) {
                body.screenValidation("final", "#final", ReportTestScreenshots.SECOND_PIXEL_PNG,
                        List.of(), List.of(), ReportScreenshotPolicy.CapturePoint.FINAL,
                        AtfReportStatus.PASS);
            }
            if (failure) {
                body.failureEvidence("failure", "#failure", List.of(),
                        new AssertionError("controlled failure"), ReportTestScreenshots.ONE_PIXEL_PNG);
            }
            body.complete(failure ? AtfReportStatus.FAIL : AtfReportStatus.PASS);
            scenario.complete(failure ? AtfReportStatus.FAIL : AtfReportStatus.PASS);
        }
        return Files.readString(report);
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
