package atf.reporting.api.contract.internal;

import atf.reporting.api.ReportQueryParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared normalization primitives for report contract validators. */
public final class ApiReportContractValueSupport {
    public static final ObjectMapper JSON = new ObjectMapper();

    private ApiReportContractValueSupport() { }

    public static boolean hasPayload(String body) {
        return body != null && !body.isBlank();
    }

    public static boolean isJson(String body) {
        if (body == null || body.isBlank()) return false;
        try {
            return JSON.readTree(body) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean sameJson(String expected, String actual) {
        try {
            return JSON.readTree(expected).equals(JSON.readTree(actual));
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String value(Map<String, String> fields, String key) {
        return fields == null ? "" : fields.getOrDefault(key, "");
    }

    public static String caseInsensitiveValue(Map<String, String> values, String key) {
        if (values == null || key == null) return null;
        return values.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    public static Map<String, String> parseMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        String source = value.trim();
        if ((source.startsWith("{") && source.endsWith("}"))
                || (source.startsWith("[") && source.endsWith("]"))) {
            source = source.substring(1, source.length() - 1).trim();
        }
        if (source.isBlank()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (String token : source.split(",\\s*")) {
            int equals = token.indexOf('=');
            if (equals <= 0) continue;
            result.put(token.substring(0, equals).trim(), token.substring(equals + 1).trim());
        }
        return result;
    }

    public static ReportQueryParameters queryFromUrl(String requestUrl) {
        if (requestUrl == null || requestUrl.isBlank()) return ReportQueryParameters.empty();
        try {
            return ReportQueryParameters.fromUri(URI.create(requestUrl.trim()));
        } catch (IllegalArgumentException ignored) {
            return ReportQueryParameters.empty();
        }
    }

    public static String compact(JsonNode node) {
        try {
            String value = JSON.writeValueAsString(node);
            return value.length() <= 360 ? value : value.substring(0, 360) + "…";
        } catch (Exception error) {
            return String.valueOf(node);
        }
    }
}
