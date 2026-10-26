package atf.reporting.configuration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.List;

/**
 * Immutable, allowlisted configuration snapshot for Overview/System.
 *
 * <p>The snapshot is intentionally smaller than a YAML dump. Values remain
 * semantic and bounded by the consumer; credential redaction is still owned
 * by {@link ReportEnvironment} at the publication boundary.</p>
 */
public record ReportEnvironmentSnapshot(Map<ReportEnvironmentField, String> values) {
    public static final String MIXED_VALUE = "Mixed — varies by scenario";
    public ReportEnvironmentSnapshot {
        Objects.requireNonNull(values, "values");
        var copy = new EnumMap<ReportEnvironmentField, String>(ReportEnvironmentField.class);
        values.forEach((field, value) -> {
            if (field == null || value == null || value.isBlank()) return;
            copy.put(field, value.trim());
        });
        values = Collections.unmodifiableMap(copy);
    }

    public static ReportEnvironmentSnapshot empty() {
        return new ReportEnvironmentSnapshot(Map.of());
    }

    /** A shared artifact may claim only metadata common to every included scenario. */
    public static ReportEnvironmentSnapshot common(Collection<ReportEnvironmentSnapshot> snapshots) {
        var inputs = List.copyOf(Objects.requireNonNull(snapshots, "snapshots"));
        var result = new EnumMap<ReportEnvironmentField, String>(ReportEnvironmentField.class);
        for (var snapshot : inputs) {
            for (var entry : snapshot.values().entrySet()) {
                String value = entry.getValue();
                boolean shared = inputs.stream().allMatch(other -> value.equals(other.values().get(entry.getKey())));
                result.put(entry.getKey(), shared ? value : MIXED_VALUE);
            }
        }
        return new ReportEnvironmentSnapshot(result);
    }

    /** Returns stable display labels without exposing the enum vocabulary. */
    public Map<String, String> displayValues() {
        var display = new LinkedHashMap<String, String>();
        values.forEach((field, value) -> display.put(field.label(), value));
        return Collections.unmodifiableMap(display);
    }

    /**
     * Returns only operational metadata allowed on Overview/System.
     *
     * <p>The full typed snapshot may retain composition context for tests and
     * diagnostics, but the published System surface must not become a second
     * test index or leak business-flow identity.</p>
     */
    public Map<String, String> systemDisplayValues() {
        var display = new LinkedHashMap<String, String>();
        values.forEach((field, value) -> {
            if (field.isSystemVisible()) {
                display.put(field.label(), value);
            }
        });
        return Collections.unmodifiableMap(display);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final EnumMap<ReportEnvironmentField, String> values =
                new EnumMap<>(ReportEnvironmentField.class);

        public Builder put(ReportEnvironmentField field, String value) {
            Objects.requireNonNull(field, "field");
            if (value == null || value.isBlank()) {
                values.remove(field);
            } else {
                values.put(field, value.trim());
            }
            return this;
        }

        public ReportEnvironmentSnapshot build() {
            return new ReportEnvironmentSnapshot(values);
        }
    }
}
