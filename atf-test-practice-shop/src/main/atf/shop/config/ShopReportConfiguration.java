package atf.shop.config;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;

import java.util.Objects;

/** Report-only policy owned by the Practice Shop consumer. */
public record ShopReportConfiguration(int systemBodyPreviewCharacters,
                                      ReportScreenshotPolicy screenshotPolicy,
                                      ReportEvidenceFlowMode flowMode,
                                      ReportUiActionDetailsMode actionDetailsMode) {
    public static final int DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS = 100;

    public ShopReportConfiguration(int systemBodyPreviewCharacters) {
        this(systemBodyPreviewCharacters, ReportScreenshotPolicy.EACH_CHECKPOINT);
    }

    public ShopReportConfiguration(int systemBodyPreviewCharacters, ReportScreenshotPolicy screenshotPolicy) {
        this(systemBodyPreviewCharacters, screenshotPolicy, ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    public ShopReportConfiguration {
        if (systemBodyPreviewCharacters <= 0) {
            throw new IllegalArgumentException(
                    "reporting.systemBodyPreviewCharacters must be positive");
        }
        screenshotPolicy = Objects.requireNonNull(screenshotPolicy, "screenshotPolicy");
        flowMode = Objects.requireNonNull(flowMode, "flowMode");
        actionDetailsMode = Objects.requireNonNull(actionDetailsMode, "actionDetailsMode");
    }

    public static ShopReportConfiguration defaults() {
        return new ShopReportConfiguration(DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS,
                ReportScreenshotPolicy.EACH_CHECKPOINT);
    }
}
