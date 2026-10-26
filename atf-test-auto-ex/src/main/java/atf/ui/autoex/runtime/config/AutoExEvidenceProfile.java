package atf.ui.autoex.runtime.config;

import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.api.reporting.evidence.profile.ReportEvidenceProfile;

/** AutoEx browser profile with resolved Reporting inputs, independent of test annotations. */
public final class AutoExEvidenceProfile {
    private AutoExEvidenceProfile() { }
    public static ReportEvidenceProfile forRun(ReportEvidenceFlowMode flow, ReportScreenshotPolicy screenshots) {
        return ReportEvidenceProfile.builder().flowMode(flow).screenshotPolicy(screenshots)
                .captureSource(ReportEvidenceCaptureSource.BROWSER).build();
    }
}
