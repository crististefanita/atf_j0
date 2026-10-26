package atf.reporting.evidence.profile;

import java.util.Locale;
import java.util.Objects;

/**
 * Describes how a consumer groups evidence in a report. It is independent
 * from the screenshot policy: a unified flow may still publish no screenshot.
 */
public enum ReportEvidenceFlowMode {
    SCREEN_CHECKPOINTS("screen-checkpoints"),
    UNIFIED_FLOW("unified-flow");

    private final String configValue;

    ReportEvidenceFlowMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    /** Parses only the documented configuration values. */
    public static ReportEvidenceFlowMode fromConfig(String value) {
        String normalized = Objects.requireNonNull(value, "flowMode")
                .trim().toLowerCase(Locale.ROOT);
        for (ReportEvidenceFlowMode mode : values()) {
            if (mode.configValue.equals(normalized)) return mode;
        }
        throw new IllegalArgumentException("Unsupported flowMode '" + value
                + "'; expected one of screen-checkpoints, unified-flow");
    }
}
