package atf.mock;

import java.util.Arrays;
import java.util.Locale;

/** Closed vocabulary for request-body behavior declared by mock YAML routes. */
public enum MockRequestBodyMode {
    JSON("json"),
    WRONG_CONTENT_TYPE("wrong-content-type");

    private final String value;

    MockRequestBodyMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** Normalizes the YAML value while preserving the existing serialized contract. */
    public static String normalize(String raw) {
        return from(raw).value();
    }

    public static MockRequestBodyMode from(String raw) {
        String normalized = raw == null || raw.isBlank()
                ? JSON.value
                : raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "unsupported mock request-body-mode: " + raw));
    }
}
