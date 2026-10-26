package atf.core.routing;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Generic, transport-neutral path template used by any API consumer.
 *
 * <p>The core owns the mechanics of joining and resolving path segments; it
 * deliberately does not own a product's concrete endpoint catalog.</p>
 */
public record ApiPathTemplate(String value) {
    public ApiPathTemplate {
        value = normalize(value);
    }

    public static ApiPathTemplate of(String value) {
        return new ApiPathTemplate(value);
    }

    public ApiPathTemplate child(String segment) {
        String normalized = Objects.requireNonNull(segment, "segment").trim();
        if (normalized.isEmpty() || normalized.contains("?")) {
            throw new IllegalArgumentException("path segment must be non-empty and must not contain a query");
        }
        return new ApiPathTemplate(value + "/" + trimSlashes(normalized));
    }

    public String resolve(Map<String, ?> values) {
        String resolved = value;
        for (var entry : Objects.requireNonNull(values, "values").entrySet()) {
            String token = "{" + entry.getKey() + "}";
            String encoded = encodeSegment(entry.getValue());
            resolved = resolved.replace(token, encoded);
        }
        if (resolved.contains("{") || resolved.contains("}")) {
            throw new IllegalArgumentException("unresolved path token in " + resolved);
        }
        return resolved;
    }

    public String resolve(String token, Object value) {
        return resolve(Map.of(Objects.requireNonNull(token, "token"), value));
    }

    private static String normalize(String raw) {
        String normalized = Objects.requireNonNull(raw, "value").trim();
        if (normalized.isEmpty() || !normalized.startsWith("/")) {
            throw new IllegalArgumentException("path template must start with '/'");
        }
        if (normalized.contains("//") || normalized.contains("#")) {
            throw new IllegalArgumentException("path template must contain one path and no fragment");
        }
        return normalized.length() > 1 && normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private static String trimSlashes(String value) {
        return value.replaceAll("^/+|/+$", "");
    }

    private static String encodeSegment(Object value) {
        String text = Objects.requireNonNull(value, "path value").toString().trim();
        if (text.isEmpty() || text.contains("/")) {
            throw new IllegalArgumentException("path value must be a non-empty segment");
        }
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
