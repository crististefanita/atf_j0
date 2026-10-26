package atf.reporting.ui;

import atf.testkit.AtfComponentTest;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.support.ReportTestScreenshots;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression proof: unified checkpoints keep separate screenshots after their shared action table. */
@AtfComponentTest
class ExtentUnifiedFlowMultipleScreenshotTest {
    @Test
    void publishesEveryCheckpointScreenshotWithoutCombiningThemIntoOneActionSequence() throws Exception {
        var output = Files.createTempDirectory("atf-unified-flow-screenshots-");
        var profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT)
                .captureSource(ReportEvidenceCaptureSource.BROWSER)
                .build();
        var report = UiExtentReportSession.openConfigured(output, "unified-flow.html",
                AtfConsoleSink.noOp(), profile);
        var reportPath = report.reportPath();
        var scenario = report.startScenario("PurchaseTest", "atf.tests", "purchase flow", List.of("ui"));
        var phase = scenario.startPhase("TEST BODY");
        phase.uiValidation("product is selected", "/products/1",
                ReportTestScreenshots.ONE_PIXEL_PNG,
                List.of(new AtfAssertion("Product", "Blue Top", "Blue Top", true)),
                List.of(new AtfUiActionRecord(1, "Click", "a.product", "details open",
                        "click completed", 8, AtfReportStatus.PASS)), AtfReportStatus.PASS);
        phase.uiValidation("cart preserves quantity", "/view_cart",
                ReportTestScreenshots.SECOND_PIXEL_PNG,
                List.of(new AtfAssertion("Quantity", "2", "2", true)),
                List.of(new AtfUiActionRecord(2, "Click", "a.cart", "cart opens",
                        "click completed", 4, AtfReportStatus.PASS)), AtfReportStatus.PASS);

        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
        report.close();

        String html = Files.readString(reportPath);
        assertTrue(html.contains("product is selected"));
        assertTrue(html.contains("cart preserves quantity"));
        assertTrue(html.contains("atf-ui-action-sequence"));
        assertEquals(2, occurrences(html, "data-atf-screenshot=\"base64\""));
        assertEquals(1, occurrences(html, "class=\"atf-ui-action-table\""),
                "all checkpoint actions share one ordered action table");
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
