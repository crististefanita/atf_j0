package atf.reporting.api;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public record ReportQueryParameters(List<ReportQueryParameter> values) {
    public ReportQueryParameters {
        values = List.copyOf(values == null ? List.of() : values);
    }

    public static ReportQueryParameters empty() { return new ReportQueryParameters(List.of()); }

    /** Prefer ordered pairs; the legacy map view cannot preserve repeated names. */
    public static ReportQueryParameters from(Object source) {
        if (source == null) return empty();
        if (source instanceof ReportQueryParameters value) return value;
        if (source instanceof Map<?, ?> values) return fromMap(values, source, "queryParameters");
        try {
            return fromValues(source.getClass().getMethod("values").invoke(source), source);
        } catch (NoSuchMethodException absent) {
            return fromLegacyMap(source);
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (error instanceof ReportModelAdapterException adapter) throw adapter;
            throw adapterFailure(source, "values", "could not be read", unwrap(error));
        }
    }

    private static ReportQueryParameters fromLegacyMap(Object source) {
        try {
            Object map = source.getClass().getMethod("asMap").invoke(source);
            if (!(map instanceof Map<?, ?> values)) {
                throw adapterFailure(source, "asMap", "must return Map");
            }
            return fromMap(values, source, "asMap");
        } catch (NoSuchMethodException error) {
            throw adapterFailure(source, "asMap", "method is missing", error);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException error) {
            Throwable cause = error instanceof InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : error;
            if (error instanceof ReportModelAdapterException adapterError) throw adapterError;
            throw adapterFailure(source, "asMap", "could not be read", cause);
        }
    }

    private static ReportQueryParameters fromValues(Object value, Object source) {
        if (!(value instanceof Collection<?> pairs)) throw adapterFailure(source, "values", "must return Collection");
        List<ReportQueryParameter> result = new ArrayList<>();
        for (Object pair : pairs) {
            if (pair == null) throw adapterFailure(source, "values", "contains a null pair");
            result.add(pair instanceof ReportQueryParameter typed ? typed
                    : new ReportQueryParameter(text(pair, "name"), text(pair, "value")));
        }
        return new ReportQueryParameters(result);
    }

    private static String text(Object source, String accessor) {
        try {
            Object value = source.getClass().getMethod(accessor).invoke(source);
            if (!(value instanceof String text)) throw adapterFailure(source, accessor, "must return text");
            return text;
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (error instanceof ReportModelAdapterException adapter) throw adapter;
            throw adapterFailure(source, accessor, "could not be read", unwrap(error));
        }
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof InvocationTargetException invocation ? invocation.getCause() : error;
    }

    private static ReportQueryParameters fromMap(Map<?, ?> values, Object source, String field) {
        List<ReportQueryParameter> parameters = new ArrayList<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String name) || name.isBlank()) {
                throw adapterFailure(source, field, "contains a non-text or blank name");
            }
            if (!(entry.getValue() instanceof String value)) {
                throw adapterFailure(source, field, "contains a non-text value for '" + name + "'");
            }
            parameters.add(new ReportQueryParameter(name, value));
        }
        return new ReportQueryParameters(parameters);
    }

    private static ReportModelAdapterException adapterFailure(Object source, String field,
                                                               String detail) {
        return adapterFailure(source, field, detail, null);
    }

    private static ReportModelAdapterException adapterFailure(Object source, String field,
                                                               String detail, Throwable cause) {
        String type = source == null ? "<null>" : source.getClass().getName();
        return new ReportModelAdapterException(
                "Cannot adapt " + type + "." + field + ": " + detail,
                type, field, cause);
    }
    public static ReportQueryParameters fromUri(URI uri) {
        if (uri == null || uri.getRawQuery() == null || uri.getRawQuery().isBlank()) return empty();
        return new ReportQueryParameters(Arrays.stream(uri.getRawQuery().split("&", -1))
                .filter(token -> !token.isBlank()).map(token -> token.split("=", 2))
                .map(pair -> new ReportQueryParameter(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "")).toList());
    }

    /** Compatibility view: the last occurrence wins; matching uses the full pair list. */
    public Map<String, String> asMap() {
        return values.stream().collect(Collectors.toMap(ReportQueryParameter::name, ReportQueryParameter::value,
                (previous, current) -> current, LinkedHashMap::new));
    }

    public boolean isEmpty() { return values.isEmpty(); }

    /** Multiset containment preserves both distinct values and repeated identical pairs. */
    public boolean containsAll(ReportQueryParameters expected) {
        Objects.requireNonNull(expected, "expected");
        Map<ReportQueryParameter, Integer> remaining = new HashMap<>();
        values.forEach(pair -> remaining.merge(pair, 1, Integer::sum));
        for (ReportQueryParameter pair : expected.values) {
            int count = remaining.getOrDefault(pair, 0);
            if (count == 0) return false;
            remaining.put(pair, count - 1);
        }
        return true;
    }

    public String display() {
        return values.stream().map(pair -> pair.name() + "=" + pair.value()).collect(Collectors.joining("&"));
    }
}
