package atf.reporting.evidence;

import java.util.Locale;
import java.util.Objects;

/**
 * Typed capture policy shared by UI evidence publishers and static contracts.
 *
 * <p>The policy is deliberately renderer- and browser-neutral. A caller can
 * decide whether a capture belongs to a checkpoint, the final state or a
 * failure, while the reporting module decides how the accepted evidence is
 * rendered. {@link #EACH_CHECKPOINT} also preserves failure evidence for the
 * backwards-compatible default behaviour.</p>
 */
public enum ReportScreenshotPolicy {
    EACH_CHECKPOINT("each-checkpoint"),
    FINAL_ONLY("final-only"),
    FAILURE_ONLY("failure-only"),
    FINAL_AND_FAILURE("final-and-failure"),
    NONE("none");

    private final String configValue;

    ReportScreenshotPolicy(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    /** Returns whether a capture at the supplied lifecycle point is publishable. */
    public boolean captures(CapturePoint point) {
        Objects.requireNonNull(point, "point");
        return switch (this) {
            case EACH_CHECKPOINT -> point == CapturePoint.CHECKPOINT
                    || point == CapturePoint.FINAL
                    || point == CapturePoint.FAILURE;
            case FINAL_ONLY -> point == CapturePoint.FINAL;
            case FAILURE_ONLY -> point == CapturePoint.FAILURE;
            case FINAL_AND_FAILURE -> point == CapturePoint.FINAL
                    || point == CapturePoint.FAILURE;
            case NONE -> false;
        };
    }

    /** Parses only the documented configuration values; no fuzzy matching. */
    public static ReportScreenshotPolicy fromConfig(String value) {
        String normalized = Objects.requireNonNull(value, "screenshotPolicy")
                .trim().toLowerCase(Locale.ROOT);
        for (ReportScreenshotPolicy policy : values()) {
            if (policy.configValue.equals(normalized)) return policy;
        }
        throw new IllegalArgumentException("Unsupported screenshotPolicy '" + value
                + "'; expected one of each-checkpoint, final-only, failure-only, "
                + "final-and-failure, none");
    }

    /** Lifecycle points are intentionally explicit so final evidence is never inferred. */
    public enum CapturePoint {
        CHECKPOINT,
        FINAL,
        FAILURE
    }
}
