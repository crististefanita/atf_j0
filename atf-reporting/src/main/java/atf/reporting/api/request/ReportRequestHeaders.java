package atf.reporting.api.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Header evidence is complete at the producer boundary; rendering never invents headers. */
public record ReportRequestHeaders(Source source, Map<String, String> values) {
    public enum Source { OBSERVED, CLIENT_CONFIGURED }

    public ReportRequestHeaders {
        source = Objects.requireNonNull(source, "source");
        values = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(values, "values")));
    }

    public static ReportRequestHeaders observed(Map<String, String> captured) {
        return new ReportRequestHeaders(Source.OBSERVED, captured == null ? Map.of() : captured);
    }

    public static ReportRequestHeaders configured(Map<String, String> defaults, Map<String, String> overrides) {
        var values = new LinkedHashMap<String, String>();
        if (defaults != null) defaults.forEach((name, value) -> put(values, name, value));
        if (overrides != null) overrides.forEach((name, value) -> put(values, name, value));
        return new ReportRequestHeaders(Source.CLIENT_CONFIGURED, values);
    }

    private static void put(Map<String, String> values, String name, String value) {
        Objects.requireNonNull(name, "header name");
        Objects.requireNonNull(value, "header value");
        values.keySet().removeIf(existing -> existing.equalsIgnoreCase(name));
        values.put(name, value);
    }
}
