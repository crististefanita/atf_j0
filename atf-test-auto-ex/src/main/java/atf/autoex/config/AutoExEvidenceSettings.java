package atf.autoex.config;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.autoex.runtime.config.AutoExEvidenceProfile;

import java.util.Objects;
import java.util.Optional;

/** Consumer-owned configured axes; an absent screenshot override inherits Reporting configuration. */
public record AutoExEvidenceSettings(ReportEvidenceFlowMode flowMode,
                                      Optional<ReportScreenshotPolicy> screenshots,
                                      ReportUiActionDetailsMode actionDetailsMode) {
    public AutoExEvidenceSettings {
        Objects.requireNonNull(flowMode, "flowMode");
        Objects.requireNonNull(screenshots, "screenshots");
        Objects.requireNonNull(actionDetailsMode, "actionDetailsMode");
    }

    public static AutoExEvidenceSettings defaults() {
        return new AutoExEvidenceSettings(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS, Optional.empty(),
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    public ReportEvidenceProfile profile(ReportScreenshotPolicy inherited) {
        return AutoExEvidenceProfile.forRun(flowMode, screenshots.orElse(Objects.requireNonNull(inherited, "inherited")))
                .toBuilder().actionDetailsMode(actionDetailsMode).build();
    }
}
