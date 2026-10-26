package atf.reporting.evidence.profile;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.support.ReportTestScreenshots;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportEvidenceProfileTest {
    @ParameterizedTest
    @EnumSource(ReportEvidenceCaptureSource.class)
    void noneFiltersEveryCapturePointBeforeCallingAnyProvider(ReportEvidenceCaptureSource source) {
        var calls = new AtomicInteger();
        var provider = provider(source, ReportTestScreenshots.ONE_PIXEL_PNG, calls);
        var disabled = ReportEvidenceProfile.builder().captureSource(source)
                .screenshotPolicy(ReportScreenshotPolicy.NONE).build();

        for (var point : ReportScreenshotPolicy.CapturePoint.values()) {
            assertTrue(disabled.capture(point, "disabled", "#target", provider).isEmpty());
        }
        assertEquals(0, calls.get(), "disabled captures must never invoke a browser or mock provider");

        var enabled = disabled.toBuilder().screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT).build();
        assertTrue(enabled.capture(ReportScreenshotPolicy.CapturePoint.CHECKPOINT,
                "enabled", "#target", provider).isPresent());
        assertEquals(1, calls.get(), "a permitted capture must still reach the same provider");
    }

    @Test
    void mapsTheThreeIndependentEvidenceAxes() {
        var profile = ReportEvidenceProfile.fromConfig(
                "unified-flow", "final-and-failure", "mock", "summary-only");

        assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, profile.flowMode());
        assertEquals(ReportScreenshotPolicy.FINAL_AND_FAILURE, profile.screenshotPolicy());
        assertEquals(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK, profile.captureSource());
        assertEquals("unified-flow", profile.flowMode().configValue());
        assertEquals("mock", profile.captureSource().configValue());
        assertEquals(ReportUiActionDetailsMode.SUMMARY_ONLY, profile.actionDetailsMode());
    }

    @Test
    void builderAndRoundTripKeepTheProfileImmutable() {
        var profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .captureSource(ReportEvidenceCaptureSource.BROWSER)
                .actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY)
                .build();

        assertEquals(profile, profile.toBuilder().build());
        assertTrue(profile.captures(ReportScreenshotPolicy.CapturePoint.FINAL));
        assertEquals(ReportUiActionDetailsMode.SUMMARY_ONLY, profile.toBuilder().build().actionDetailsMode());
    }

    @Test
    void rejectsUnknownVocabularyInsteadOfGuessing() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportEvidenceProfile.fromConfig("unified", "final-only", "mock"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportEvidenceProfile.fromConfig("unified-flow", "final-only", "fixture"));
    }

    @Test
    void filtersBeforeCallingTheProviderAndRequiresProvenanceAndValidPng() {
        var profile = ReportEvidenceProfile.fromConfig(
                "unified-flow", "final-only", "mock");
        var calls = new AtomicInteger();
        ReportEvidenceCaptureProvider valid = provider(
                ReportEvidenceCaptureSource.DETERMINISTIC_MOCK,
                ReportTestScreenshots.ONE_PIXEL_PNG, calls);

        assertTrue(profile.capture(ReportScreenshotPolicy.CapturePoint.CHECKPOINT,
                "checkpoint", "#checkpoint", valid).isEmpty());
        assertEquals(0, calls.get());
        assertEquals("final", profile.capture(ReportScreenshotPolicy.CapturePoint.FINAL,
                "final", "#final", valid).orElseThrow().title());
        assertEquals(1, calls.get());
        assertThrows(IllegalArgumentException.class,
                () -> profile.capture(ReportScreenshotPolicy.CapturePoint.FINAL,
                        "final", " ", valid));
        assertThrows(IllegalArgumentException.class,
                () -> profile.capture(ReportScreenshotPolicy.CapturePoint.FINAL,
                        "final", "#final", provider(ReportEvidenceCaptureSource.BROWSER,
                                ReportTestScreenshots.ONE_PIXEL_PNG, calls)));
        assertThrows(IllegalArgumentException.class,
                () -> profile.capture(ReportScreenshotPolicy.CapturePoint.FINAL,
                        "final", "#final", provider(ReportEvidenceCaptureSource.DETERMINISTIC_MOCK,
                                "AQID", calls)));
        assertEquals(2, calls.get(), "only permitted captures with matching sources reach a provider");
    }

    private static ReportEvidenceCaptureProvider provider(ReportEvidenceCaptureSource source,
                                                          String payload, AtomicInteger calls) {
        return new ReportEvidenceCaptureProvider() {
            @Override public ReportEvidenceCaptureSource source() { return source; }
            @Override public ReportScreenshot capture(String title, String target) {
                calls.incrementAndGet();
                return new ReportScreenshot(title, payload);
            }
        };
    }
}
