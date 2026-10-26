package atf.core.http.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Immutable query collection retaining order and duplicate parameter occurrences. */
public final class ApiQueryParameters {
    private final List<ApiQueryParameter> values;

    private ApiQueryParameters(Collection<ApiQueryParameter> values) {
        this.values = List.copyOf(Objects.requireNonNull(values, "values"));
    }

    public static ApiQueryParameters empty() {
        return new ApiQueryParameters(List.of());
    }

    public static ApiQueryParameters fromMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) return empty();
        return new ApiQueryParameters(values.entrySet().stream()
                .map(entry -> new ApiQueryParameter(entry.getKey(), entry.getValue()))
                .toList());
    }

    public static ApiQueryParameters of(ApiQueryParameter... values) {
        return new ApiQueryParameters(List.of(values));
    }

    public static ApiQueryParameters of(Collection<ApiQueryParameter> values) {
        return new ApiQueryParameters(new ArrayList<>(values));
    }

    /** Parses a raw URI query without collapsing repeated parameter names. */
    public static ApiQueryParameters fromRawQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return empty();
        List<ApiQueryParameter> parsed = new ArrayList<>();
        for (String token : rawQuery.split("&", -1)) {
            if (token.isBlank()) continue;
            String[] pair = token.split("=", 2);
            String name = decode(pair[0]);
            String value = pair.length == 1 ? "" : decode(pair[1]);
            parsed.add(new ApiQueryParameter(name, value));
        }
        return of(parsed);
    }

    public static ApiQueryParameters fromUri(URI uri) {
        return uri == null ? empty() : fromRawQuery(uri.getRawQuery());
    }

    public List<ApiQueryParameter> values() {
        return Collections.unmodifiableList(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Compatibility view for existing Map consumers; the last occurrence wins. */
    public Map<String, String> asMap() {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach(parameter -> result.put(parameter.name(), parameter.value()));
        return Map.copyOf(result);
    }

    public long count(String name, String value) {
        return values.stream().filter(parameter -> parameter.name().equals(name)
                && parameter.value().equals(value)).count();
    }

    /** True when this collection contains every expected occurrence. */
    public boolean containsAll(ApiQueryParameters expected) {
        if (expected == null || expected.isEmpty()) return true;
        return expected.values().stream().allMatch(parameter ->
                count(parameter.name(), parameter.value())
                        >= expected.count(parameter.name(), parameter.value()));
    }

    public String display() {
        return values.stream().map(ApiQueryParameter::toString).collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public String toString() {
        return display();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
