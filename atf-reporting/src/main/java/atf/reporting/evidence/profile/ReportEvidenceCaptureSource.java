package atf.reporting.evidence.profile;

import java.util.Locale;
import java.util.Objects;

/**
 * Renderer-neutral provenance for a screenshot payload. The reporting module
 * records the source category, while the consumer owns the actual browser or
 * mock adapter.
 */
public enum ReportEvidenceCaptureSource {
    BROWSER("browser"),
    DETERMINISTIC_MOCK("mock"),
    CUSTOM_PROVIDER("provider");

    private final String configValue;

    ReportEvidenceCaptureSource(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    /** Parses only the documented configuration values. */
    public static ReportEvidenceCaptureSource fromConfig(String value) {
        String normalized = Objects.requireNonNull(value, "captureSource")
                .trim().toLowerCase(Locale.ROOT);
        for (ReportEvidenceCaptureSource source : values()) {
            if (source.configValue.equals(normalized)) return source;
        }
        throw new IllegalArgumentException("Unsupported captureSource '" + value
                + "'; expected one of browser, mock, provider");
    }
}
