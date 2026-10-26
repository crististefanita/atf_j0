package atf.reporting.api;

import java.util.Locale;
import java.util.Map;

/**
 * Stable, credential-free vocabulary for the authentication mechanism observed
 * on one request.  The enum describes evidence, not whether a credential was
 * valid or whether an identity was authorized.
 */
public enum ReportAuthMethod {
    NONE("none"),
    BEARER("bearer"),
    COOKIE("cookie"),
    API_FUNCTION_KEY("api/function-key"),
    SIGNED_URL("signed-url"),
    TOKEN_ACQUISITION("token-acquisition"),
    BASIC("basic"),
    OTHER_UNKNOWN("other/unknown");

    private final String wireValue;

    ReportAuthMethod(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    /** Classifies a consumer-provided auth profile without exposing its value. */
    public static ReportAuthMethod fromProfile(String profile) {
        String normalized = normalize(profile);
        if (normalized.isBlank() || normalized.equals("none")) return NONE;
        if (normalized.contains("bearer")) return BEARER;
        if (normalized.contains("cookie")) return COOKIE;
        if (normalized.contains("api-key") || normalized.contains("api_key")
                || normalized.contains("apikey") || normalized.contains("function-key")
                || normalized.contains("function_key")) return API_FUNCTION_KEY;
        if (normalized.contains("signed") || normalized.contains("signature")) return SIGNED_URL;
        if (normalized.contains("token")) return TOKEN_ACQUISITION;
        if (normalized.contains("basic")) return BASIC;
        return OTHER_UNKNOWN;
    }

    /** Classifies header presence and scheme only; values are never returned. */
    public static ReportAuthMethod fromHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return NONE;
        String authorization = value(headers, "authorization");
        if (authorization != null) {
            String scheme = normalize(authorization).split("\\s+", 2)[0];
            return switch (scheme) {
                case "bearer" -> BEARER;
                case "basic" -> BASIC;
                case "token" -> TOKEN_ACQUISITION;
                default -> OTHER_UNKNOWN;
            };
        }
        if (contains(headers, "cookie")) return COOKIE;
        if (contains(headers, "x-api-key") || contains(headers, "api-key")
                || contains(headers, "function-key")) return API_FUNCTION_KEY;
        return NONE;
    }

    /** Classifies only well-known signed-URL parameter names. */
    public static ReportAuthMethod fromQuery(Map<String, String> query) {
        if (query == null || query.isEmpty()) return NONE;
        return query.keySet().stream().map(ReportAuthMethod::normalize)
                .anyMatch(key -> key.equals("signature") || key.equals("sig")
                        || key.equals("x-amz-signature") || key.equals("x-amz-credential")
                        || key.equals("expires") || key.equals("x-amz-expires"))
                ? SIGNED_URL : NONE;
    }

    private static boolean contains(Map<String, String> values, String name) {
        return values.keySet().stream().anyMatch(key -> key != null && key.equalsIgnoreCase(name));
    }

    private static String value(Map<String, String> values, String name) {
        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
