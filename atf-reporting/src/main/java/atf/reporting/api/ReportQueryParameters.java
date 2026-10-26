package atf.reporting.api;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public record ReportQueryParameters(List<ReportQueryParameter> values) {
    public ReportQueryParameters { values=List.copyOf(values==null?List.of():values); }
    public static ReportQueryParameters empty(){return new ReportQueryParameters(List.of());}
    public static ReportQueryParameters from(Object source) {
        if (source == null) return empty();
        if (source instanceof ReportQueryParameters value) return value;
        if (source instanceof Map<?, ?> values) return fromMap(values, source, "queryParameters");
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
    public static ReportQueryParameters fromUri(URI uri){
        if(uri==null||uri.getRawQuery()==null||uri.getRawQuery().isBlank()) return empty();
        return new ReportQueryParameters(Arrays.stream(uri.getRawQuery().split("&"))
            .map(item->item.split("=",2)).map(parts->new ReportQueryParameter(URLDecoder.decode(parts[0],StandardCharsets.UTF_8), parts.length>1?URLDecoder.decode(parts[1],StandardCharsets.UTF_8):"" )).toList());
    }
    public Map<String,String> asMap(){return values.stream().collect(Collectors.toMap(ReportQueryParameter::name,ReportQueryParameter::value,(a,b)->b,LinkedHashMap::new));}
    public boolean isEmpty() { return values.isEmpty(); }
    public boolean containsAll(ReportQueryParameters expected) { return asMap().entrySet().containsAll(expected.asMap().entrySet()); }
    public String display() { return asMap().entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).collect(Collectors.joining("&")); }
}
