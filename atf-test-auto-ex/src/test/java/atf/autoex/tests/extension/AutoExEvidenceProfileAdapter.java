package atf.autoex.tests.extension;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.testkit.evidence.AtfUiEvidencePlan;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import java.util.Objects;

/** Builds the browser-evidence profile shared by the report session and each E2E run. */
public final class AutoExEvidenceProfileAdapter {
    private AutoExEvidenceProfileAdapter() { }

    public static ReportEvidenceProfile forRun(AtfUiEvidencePlan declaration, ReportEvidenceProfile configured) {
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(configured, "configured");
        ReportEvidenceProfile mapped = forRun(declaration.flow(), declaration.screenshots(), configured.screenshotPolicy());
        return mapped.toBuilder()
                .flowMode(declaration.flow() == AtfUiEvidenceFlow.CONFIGURED ? configured.flowMode() : mapped.flowMode())
                .actionDetailsMode(switch (declaration.actionDetails()) {
                    case CONFIGURED -> configured.actionDetailsMode();
                    case INCLUDED_COLLAPSED -> ReportUiActionDetailsMode.INCLUDED_COLLAPSED;
                    case SUMMARY_ONLY -> ReportUiActionDetailsMode.SUMMARY_ONLY;
                }).build();
    }

    public static ReportEvidenceProfile forRun(AtfUiEvidenceFlow flow,
                                               AtfUiScreenshotPolicy screenshots,
                                               ReportScreenshotPolicy configuredPolicy) {
        Objects.requireNonNull(flow, "flow");
        Objects.requireNonNull(screenshots, "screenshots");
        Objects.requireNonNull(configuredPolicy, "configuredPolicy");
        ReportScreenshotPolicy selectedPolicy = switch (screenshots) {
            case CONFIGURED -> configuredPolicy;
            case EACH_CHECKPOINT -> ReportScreenshotPolicy.EACH_CHECKPOINT;
            case FINAL_ONLY -> ReportScreenshotPolicy.FINAL_ONLY;
            case FAILURE_ONLY -> ReportScreenshotPolicy.FAILURE_ONLY;
            case FINAL_AND_FAILURE -> ReportScreenshotPolicy.FINAL_AND_FAILURE;
            case NONE -> ReportScreenshotPolicy.NONE;
        };
        ReportEvidenceFlowMode flowMode = switch (flow) {
            case CONFIGURED, SCREEN_CHECKPOINTS -> ReportEvidenceFlowMode.SCREEN_CHECKPOINTS;
            case UNIFIED_FLOW -> ReportEvidenceFlowMode.UNIFIED_FLOW;
        };
        return AutoExEvidenceProfile.forRun(flowMode, selectedPolicy);
    }
}
