package atf.reporting.ui.failure;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportScreenshot;

import java.util.Objects;

import static atf.reporting.evidence.ReportScreenshotPolicy.CapturePoint.FAILURE;

/** Actual publication state, not a browser action or an assertion about the UI. */
public enum UiFailureScreenshotState {
    CAPTURED("Screenshot captured; policy=%s"),
    UNAVAILABLE("Screenshot unavailable: no capture supplied; policy=%s"),
    INVALID("Screenshot unavailable: invalid Base64 PNG supplied; policy=%s"),
    DISABLED_BY_POLICY("Screenshot disabled by policy=%s");

    private final String description;

    UiFailureScreenshotState(String description) {
        this.description = description;
    }

    public static UiFailureScreenshotState resolve(ReportScreenshotPolicy policy, String base64Png) {
        Objects.requireNonNull(policy, "policy");
        if (!policy.captures(FAILURE)) return DISABLED_BY_POLICY;
        if (base64Png == null || base64Png.isBlank()) return UNAVAILABLE;
        return ReportScreenshot.hasPngSignature(base64Png) ? CAPTURED : INVALID;
    }

    public String description(ReportScreenshotPolicy policy) {
        return description.formatted(Objects.requireNonNull(policy, "policy").configValue());
    }
}
