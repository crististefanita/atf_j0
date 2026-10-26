package atf.reporting.model;

import java.util.Objects;

/**
 * Immutable key/value evidence item shared by the semantic document and its
 * renderers.
 *
 * <p>A field is data, not markup. Keeping it typed prevents renderers from
 * passing positional {@code String[]} values around and makes redaction,
 * formatting and component tests independent of HTML.</p>
 */
public record ReportField(String name, String value) {
    public ReportField {
        name = requireText(name, "name");
        value = Objects.requireNonNullElse(value, "");
    }

    public static ReportField of(String name, String value) {
        return new ReportField(name, value);
    }

    private static String requireText(String value, String field) {
        if (Objects.requireNonNull(value, field).isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
