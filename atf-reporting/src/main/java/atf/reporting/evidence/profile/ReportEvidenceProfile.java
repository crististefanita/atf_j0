package atf.reporting.evidence.profile;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportScreenshot;

import java.util.Objects;
import java.util.Optional;

/**
 * Typed, browser-neutral composition of the evidence and disclosure policies.
 *
 * <p>The profile is policy and provenance, not a browser executor. A
 * composition root maps YAML or builder input to this object and supplies
 * a browser or mock provider. Capture is filtered before that provider is
 * invoked; the resulting payload enters the existing reporting lifecycle.</p>
 */
public record ReportEvidenceProfile(ReportEvidenceFlowMode flowMode,
                                    ReportScreenshotPolicy screenshotPolicy,
                                    ReportEvidenceCaptureSource captureSource,
                                    ReportUiActionDetailsMode actionDetailsMode) {
    public ReportEvidenceProfile {
        flowMode = Objects.requireNonNull(flowMode, "flowMode");
        screenshotPolicy = Objects.requireNonNull(screenshotPolicy, "screenshotPolicy");
        captureSource = Objects.requireNonNull(captureSource, "captureSource");
        actionDetailsMode = Objects.requireNonNull(actionDetailsMode, "actionDetailsMode");
    }

    /** Backward-compatible constructor with the safe, audit-friendly default. */
    public ReportEvidenceProfile(ReportEvidenceFlowMode flowMode,
                                 ReportScreenshotPolicy screenshotPolicy,
                                 ReportEvidenceCaptureSource captureSource) {
        this(flowMode, screenshotPolicy, captureSource,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    /** Backward-compatible profile for the existing checkpoint lifecycle. */
    public static ReportEvidenceProfile defaults() {
        return new ReportEvidenceProfile(
                ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.EACH_CHECKPOINT,
                ReportEvidenceCaptureSource.CUSTOM_PROVIDER,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    public static ReportEvidenceProfile forScreenshotPolicy(ReportScreenshotPolicy policy) {
        return new ReportEvidenceProfile(
                ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                Objects.requireNonNull(policy, "screenshotPolicy"),
                ReportEvidenceCaptureSource.CUSTOM_PROVIDER,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    /** Maps the legacy three YAML values, defaulting action details to included/collapsed. */
    public static ReportEvidenceProfile fromConfig(String flowMode,
                                                   String screenshotPolicy,
                                                   String captureSource) {
        return fromConfig(flowMode, screenshotPolicy, captureSource,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED.configValue());
    }

    public static ReportEvidenceProfile fromConfig(String flowMode,
                                                   String screenshotPolicy,
                                                   String captureSource,
                                                   String actionDetailsMode) {
        return new ReportEvidenceProfile(
                ReportEvidenceFlowMode.fromConfig(flowMode),
                ReportScreenshotPolicy.fromConfig(screenshotPolicy),
                ReportEvidenceCaptureSource.fromConfig(captureSource),
                ReportUiActionDetailsMode.fromConfig(actionDetailsMode));
    }

    public boolean captures(ReportScreenshotPolicy.CapturePoint point) {
        return screenshotPolicy.captures(point);
    }

    /** Resolves one capture before publication, without invoking a filtered provider. */
    public Optional<ReportScreenshot> capture(ReportScreenshotPolicy.CapturePoint point,
                                               String title, String target,
                                               ReportEvidenceCaptureProvider provider) {
        if (!captures(point)) return Optional.empty();
        Objects.requireNonNull(provider, "provider");
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("A screenshot target is required by the selected policy");
        }
        if (provider.source() != captureSource) {
            throw new IllegalArgumentException("Capture provider source does not match the evidence profile");
        }
        ReportScreenshot screenshot = Objects.requireNonNull(
                provider.capture(title, target), "capture provider result");
        if (!ReportScreenshot.hasPngSignature(screenshot.base64Png())) {
            throw new IllegalArgumentException("Capture provider must return a Base64 PNG screenshot");
        }
        return Optional.of(screenshot);
    }

    public Builder toBuilder() {
        return builder().flowMode(flowMode)
                .screenshotPolicy(screenshotPolicy)
                .captureSource(captureSource)
                .actionDetailsMode(actionDetailsMode);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Small typed builder for YAML/composition-root mapping. */
    public static final class Builder {
        private ReportEvidenceFlowMode flowMode = ReportEvidenceFlowMode.SCREEN_CHECKPOINTS;
        private ReportScreenshotPolicy screenshotPolicy = ReportScreenshotPolicy.EACH_CHECKPOINT;
        private ReportEvidenceCaptureSource captureSource = ReportEvidenceCaptureSource.CUSTOM_PROVIDER;
        private ReportUiActionDetailsMode actionDetailsMode = ReportUiActionDetailsMode.INCLUDED_COLLAPSED;

        public Builder flowMode(ReportEvidenceFlowMode value) {
            flowMode = Objects.requireNonNull(value, "flowMode");
            return this;
        }

        public Builder screenshotPolicy(ReportScreenshotPolicy value) {
            screenshotPolicy = Objects.requireNonNull(value, "screenshotPolicy");
            return this;
        }

        public Builder captureSource(ReportEvidenceCaptureSource value) {
            captureSource = Objects.requireNonNull(value, "captureSource");
            return this;
        }

        public Builder actionDetailsMode(ReportUiActionDetailsMode value) {
            actionDetailsMode = Objects.requireNonNull(value, "actionDetailsMode");
            return this;
        }

        public ReportEvidenceProfile build() {
            return new ReportEvidenceProfile(flowMode, screenshotPolicy, captureSource, actionDetailsMode);
        }
    }
}
