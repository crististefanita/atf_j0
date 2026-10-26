package atf.shop.reporting;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;

import java.util.Objects;

/** Composes the consumer's YAML-owned screenshot policy into the generic evidence profile. */
public final class ShopScreenshotPolicy {
    private ShopScreenshotPolicy() { }

    public static ReportEvidenceProfile profile(ReportScreenshotPolicy screenshotPolicy) {
        return ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS)
                .screenshotPolicy(Objects.requireNonNull(screenshotPolicy, "screenshotPolicy"))
                .captureSource(ReportEvidenceCaptureSource.BROWSER)
                .build();
    }
}
