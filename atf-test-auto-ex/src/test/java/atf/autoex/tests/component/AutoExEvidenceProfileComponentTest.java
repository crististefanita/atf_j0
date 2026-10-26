package atf.autoex.tests.component;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.testkit.AtfComponentTest;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.testkit.evidence.AtfUiEvidencePlan;
import atf.testkit.evidence.AtfUiEvidenceResolver;
import atf.testkit.evidence.AtfUiActionDetails;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import atf.autoex.tests.e2e.AutoExPurchaseUiFinalScreenshotE2eTest;
import atf.autoex.tests.extension.AutoExEvidenceProfileAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards AutoEx flow composition and screenshot-policy mapping without a live site or browser. */
@AtfComponentTest
class AutoExEvidenceProfileComponentTest {

    @Test
    void configuredAxesAndComposedOverridesUseTheActualResolvedBase() {
        var configured = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.UNIFIED_FLOW,
                ReportScreenshotPolicy.NONE).toBuilder().actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY).build();
        assertEquals(configured, AutoExEvidenceProfileAdapter.forRun(AtfUiEvidencePlan.configured(), configured));
        var selected = AutoExEvidenceProfileAdapter.forRun(
                AtfUiEvidenceResolver.resolve(ComposedEvidence.class).orElseThrow(), configured);
        assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, selected.flowMode());
        assertEquals(ReportScreenshotPolicy.FINAL_AND_FAILURE, selected.screenshotPolicy());
        assertEquals(ReportUiActionDetailsMode.INCLUDED_COLLAPSED, selected.actionDetailsMode());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @AtfUiEvidence(screenshots = AtfUiScreenshotPolicy.FINAL_AND_FAILURE,
            actionDetails = AtfUiActionDetails.INCLUDED_COLLAPSED)
    private @interface TerminalEvidence { }
    @TerminalEvidence private static class ComposedEvidence { }

    @Test
    void annotationOverridesOnlyDeclaredAxesAndConfiguredAxesRemainConfigured() {
        var configured = AutoExEvidenceProfileAdapter.forRun(AtfUiEvidenceFlow.CONFIGURED,
                AtfUiScreenshotPolicy.CONFIGURED, ReportScreenshotPolicy.FINAL_AND_FAILURE);
        var unifiedWithConfiguredScreenshots = AutoExEvidenceProfileAdapter.forRun(
                AtfUiEvidenceFlow.UNIFIED_FLOW, AtfUiScreenshotPolicy.CONFIGURED,
                ReportScreenshotPolicy.NONE);
        var finalOnly = AutoExEvidenceProfileAdapter.forRun(AtfUiEvidenceFlow.UNIFIED_FLOW,
                AtfUiScreenshotPolicy.FINAL_ONLY, ReportScreenshotPolicy.EACH_CHECKPOINT);

        assertAll(
                () -> assertEquals(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                        configured.flowMode()),
                () -> assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW,
                        unifiedWithConfiguredScreenshots.flowMode()),
                () -> assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, finalOnly.flowMode()),
                () -> assertEquals(ReportScreenshotPolicy.FINAL_AND_FAILURE,
                        configured.screenshotPolicy()),
                () -> assertEquals(ReportScreenshotPolicy.NONE,
                        unifiedWithConfiguredScreenshots.screenshotPolicy()),
                () -> assertEquals(ReportScreenshotPolicy.FINAL_ONLY,
                        finalOnly.screenshotPolicy()),
                () -> assertEquals(ReportEvidenceCaptureSource.BROWSER,
                        finalOnly.captureSource()));
    }

    @Test
    void singleFinalScreenshotFlowAlsoCapturesFailureEvidence() {
        AtfUiEvidence annotation = AutoExPurchaseUiFinalScreenshotE2eTest.class
                .getAnnotation(AtfUiEvidence.class);

        assertEquals(AtfUiScreenshotPolicy.FINAL_AND_FAILURE, annotation.screenshots());
        var profile = AutoExEvidenceProfileAdapter.forRun(annotation.flow(), annotation.screenshots(),
                ReportScreenshotPolicy.NONE);
        assertAll(
                () -> assertTrue(profile.captures(ReportScreenshotPolicy.CapturePoint.FINAL)),
                () -> assertTrue(profile.captures(ReportScreenshotPolicy.CapturePoint.FAILURE)),
                () -> assertFalse(profile.captures(ReportScreenshotPolicy.CapturePoint.CHECKPOINT)));
    }

}
