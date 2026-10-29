package atf.reporting.ui;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.util.List;

/** Immutable checkpoint buffered until its unified business flow is published. */
record UiUnifiedFlowCheckpoint(String keyword, String title, String target,
                               String base64Png, List<AtfAssertion> assertions,
                               AtfReportStatus status, UiValidationKind validationKind,
                               List<AtfUiActionRecord> actions,
                               UiApiValidationEvidence apiValidation,
                               ReportScreenshotPolicy.CapturePoint capturePoint,
                               boolean mediaScreenshot) {
    UiUnifiedFlowCheckpoint {
        keyword = keyword == null ? "" : keyword.trim();
        title = title == null ? "" : title.trim();
        target = target == null ? "" : target.trim();
        base64Png = base64Png == null ? "" : base64Png;
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        status = new ReportStatusAggregator().resolve(status == null ? AtfReportStatus.PASS : status, assertions);
        java.util.Objects.requireNonNull(validationKind, "validationKind");
        actions = actions == null ? List.of() : List.copyOf(actions);
        java.util.Objects.requireNonNull(apiValidation, "apiValidation");
        capturePoint = capturePoint == null
                ? ReportScreenshotPolicy.CapturePoint.CHECKPOINT : capturePoint;
    }

    List<ReportExecutionEvidence> apiEvidence() {
        return apiValidation.exchanges().stream()
                .map(UiApiValidationEvidence.Exchange::evidence).toList();
    }

    String gherkinTitle() {
        return ((keyword.isBlank() ? "" : keyword + " ") + title).trim();
    }
}
