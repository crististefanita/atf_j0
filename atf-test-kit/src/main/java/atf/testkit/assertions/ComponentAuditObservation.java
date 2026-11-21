package atf.testkit.assertions;

import java.util.List;
import java.util.Objects;

/** Ordered, renderer-neutral facts attached to one audited JUnit phase. */
public record ComponentAuditObservation(String title, List<Field> fields) {
    public ComponentAuditObservation {
        title = required(title, "title");
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    }

    public record Field(String name, String value) {
        public Field {
            name = required(name, "name");
            value = Objects.requireNonNullElse(value, "");
        }
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
