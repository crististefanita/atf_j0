package atf.ui.config;

import java.util.Arrays;
import java.util.Locale;

/** Safe, read-only Playwright operations that may be retried at action scope. */
public enum UiRetryOperation {
    NAVIGATE("Navigate"),
    WAIT_FOR_VISIBLE("Wait for visible"),
    WAIT_FOR_TEXT("Wait for text"),
    WAIT_FOR_ENABLED("Wait for enabled"),
    WAIT_FOR_STATE("Wait for state"),
    WAIT_FOR_DISCLOSURE("Wait for disclosure");

    private final String label;

    UiRetryOperation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static UiRetryOperation from(String value) {
        String normalized = value == null ? "" : value.trim();
        return Arrays.stream(values())
                .filter(operation -> operation.label.equalsIgnoreCase(normalized)
                        || operation.name().equalsIgnoreCase(normalized.replace('-', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported UI retry operation: " + value));
    }

    public String configName() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
