package atf.reporting.evidence;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportScreenshotPolicyTest {
    @Test
    void exposesTheDocumentedCaptureMatrix() {
        assertAll(
                () -> assertTrue(ReportScreenshotPolicy.EACH_CHECKPOINT
                        .captures(ReportScreenshotPolicy.CapturePoint.CHECKPOINT)),
                () -> assertTrue(ReportScreenshotPolicy.EACH_CHECKPOINT
                        .captures(ReportScreenshotPolicy.CapturePoint.FAILURE)),
                () -> assertTrue(ReportScreenshotPolicy.FINAL_ONLY
                        .captures(ReportScreenshotPolicy.CapturePoint.FINAL)),
                () -> assertFalse(ReportScreenshotPolicy.FINAL_ONLY
                        .captures(ReportScreenshotPolicy.CapturePoint.CHECKPOINT)),
                () -> assertTrue(ReportScreenshotPolicy.FAILURE_ONLY
                        .captures(ReportScreenshotPolicy.CapturePoint.FAILURE)),
                () -> assertFalse(ReportScreenshotPolicy.FAILURE_ONLY
                        .captures(ReportScreenshotPolicy.CapturePoint.FINAL)),
                () -> assertTrue(ReportScreenshotPolicy.FINAL_AND_FAILURE
                        .captures(ReportScreenshotPolicy.CapturePoint.FINAL)),
                () -> assertTrue(ReportScreenshotPolicy.FINAL_AND_FAILURE
                        .captures(ReportScreenshotPolicy.CapturePoint.FAILURE)),
                () -> assertFalse(ReportScreenshotPolicy.NONE
                        .captures(ReportScreenshotPolicy.CapturePoint.CHECKPOINT)));
    }

    @Test
    void parsesOnlyCanonicalConfigurationValues() {
        assertEquals(ReportScreenshotPolicy.FINAL_AND_FAILURE,
                ReportScreenshotPolicy.fromConfig(" final-and-failure "));
        assertThrows(IllegalArgumentException.class,
                () -> ReportScreenshotPolicy.fromConfig("final"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportScreenshotPolicy.fromConfig("checkpoint-and-failure"));
    }
}
