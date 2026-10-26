package atf.autoex.tests.component;

import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.tests.e2e.AutoExAccountUiE2eTest;
import atf.autoex.tests.e2e.AutoExCartUiE2eTest;
import atf.autoex.tests.e2e.AutoExExistingCustomerUiApiE2eTest;
import atf.autoex.tests.e2e.AutoExPurchaseUiApiE2eTest;
import atf.autoex.tests.e2e.AutoExPurchaseUiE2eTest;
import atf.autoex.tests.e2e.AutoExPurchaseUiFinalScreenshotE2eTest;
import atf.autoex.tests.extension.AutoExEvidenceProfileAdapter;
import atf.autoex.tests.extension.AutoExE2eExtension;
import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.tests.suites.AutoExE2eSuite;
import atf.console.junit.AtfConsoleTestExtension;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.testkit.AtfComponentTest;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiActionDetails;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiEvidencePlan;
import atf.testkit.evidence.AtfUiEvidenceResolver;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.testkit.output.UiReportSubdirectory;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.SelectClasses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    void realConsumerAnnotationBundlesOnlySharedHarnessAndPreservesSelections() {
        var annotation = AutoExE2eTest.class;
        assertAll(
                () -> assertTrue(annotation.isAnnotationPresent(AtfE2eTest.class)),
                () -> assertEquals("auto-ex", annotation.getAnnotation(UiReportSubdirectory.class).value()),
                () -> assertEquals(AtfConsoleMessages.Policy.DETAILS,
                        annotation.getAnnotation(AtfConsoleMessages.class).value()),
                () -> assertTrue(annotation.isAnnotationPresent(AtfReportPath.class)),
                () -> assertArrayEquals(new Class<?>[]{AtfConsoleTestExtension.class, AutoExE2eExtension.class},
                        annotation.getAnnotation(ExtendWith.class).value()),
                () -> assertEquals(ExecutionMode.SAME_THREAD,
                        annotation.getAnnotation(Execution.class).value()));
        Class<?>[] selected = {
                AutoExPurchaseUiE2eTest.class, AutoExCartUiE2eTest.class,
                AutoExAccountUiE2eTest.class, AutoExExistingCustomerUiApiE2eTest.class,
                AutoExPurchaseUiApiE2eTest.class, AutoExPurchaseUiFinalScreenshotE2eTest.class};
        assertArrayEquals(selected, AutoExE2eSuite.class.getAnnotation(SelectClasses.class).value());
        assertTrue(Arrays.stream(selected)
                .allMatch(testClass -> testClass.isAnnotationPresent(AutoExE2eTest.class)));
        assertEquals(List.of(TestChannel.UI, TestChannel.UI, TestChannel.UI,
                        TestChannel.UI_API, TestChannel.UI_API, TestChannel.UI),
                Arrays.stream(selected).map(testClass ->
                        testClass.getAnnotation(AtfTestChannel.class).value()).toList());
        assertEquals(AtfUiScreenshotPolicy.FINAL_AND_FAILURE,
                AutoExPurchaseUiFinalScreenshotE2eTest.class.getAnnotation(AtfUiEvidence.class).screenshots());
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
