package atf.reporting.evidence.profile;

import java.util.Locale;
import java.util.Objects;

/** Controls how Playwright action evidence is disclosed without changing its capture. */
public enum ReportUiActionDetailsMode {
    INCLUDED_COLLAPSED("included-collapsed"),
    SUMMARY_ONLY("summary-only");

    private final String configValue;

    ReportUiActionDetailsMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static ReportUiActionDetailsMode fromConfig(String value) {
        String normalized = Objects.requireNonNull(value, "actionDetailsMode")
                .trim().toLowerCase(Locale.ROOT);
        for (ReportUiActionDetailsMode mode : values()) {
            if (mode.configValue.equals(normalized)) return mode;
        }
        throw new IllegalArgumentException("Unknown action details mode: " + value);
    }
}
