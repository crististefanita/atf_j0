package atf.reporting.model;

import java.util.Objects;
import java.util.Locale;

/** Immutable body evidence; the historical JSON name and constructor remain compatible. */
public record ReportJsonBody(String label, String body, Direction direction, Format format) {
    public enum Direction { REQUEST, RESPONSE }
    public enum Format { JSON, FORM, TEXT }

    /** Compatibility for the original JSON port; classification happens at ingress only. */
    public ReportJsonBody(String label, String body) {
        this(label, body, label != null && label.trim().toUpperCase(Locale.ROOT)
                .startsWith("REQUEST") ? Direction.REQUEST : Direction.RESPONSE, Format.JSON);
    }

    public ReportJsonBody {
        label = requireText(label, "label");
        body = Objects.requireNonNullElse(body, "");
        direction = Objects.requireNonNull(direction, "direction");
        format = Objects.requireNonNull(format, "format");
    }

    public boolean isPresent() {
        return !body.isEmpty();
    }

    private static String requireText(String value, String field) {
        if (Objects.requireNonNull(value, field).isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
