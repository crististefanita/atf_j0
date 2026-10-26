package atf.reporting.api.contract;

import java.util.Locale;

/** Closed vocabulary for the request-body contract represented by a report. */
public enum ApiReportRequestBodyMode {
    JSON("json"),
    WRONG_CONTENT_TYPE("wrong-content-type");

    private final String value;

    ApiReportRequestBodyMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ApiReportRequestBodyMode from(String raw) {
        String normalized = raw == null || raw.isBlank()
                ? JSON.value
                : raw.trim().toLowerCase(Locale.ROOT);
        for (ApiReportRequestBodyMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unsupported report request-body mode: " + raw);
    }
}
