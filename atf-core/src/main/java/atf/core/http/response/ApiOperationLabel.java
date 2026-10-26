package atf.core.http.response;
import atf.core.http.request.HttpMethod;

/** Formats stable, human-readable operation labels from canonical API metadata. */
public final class ApiOperationLabel {
    private ApiOperationLabel() { }

    public static String format(HttpMethod method, String path, String description) {
        if (method == null) throw new IllegalArgumentException("method must not be null");
        return format(method.value(), path, description);
    }

    /**
     * Formats a report-safe operation label without requiring a core enum at
     * every boundary. The method and relative path remain protocol metadata;
     * the optional description is the short human intent shown in collapsed
     * report cards.
     */
    public static String format(String method, String path, String description) {
        String normalizedMethod = required(method, "method");
        String normalizedPath = required(path, "path");
        String operation = String.format("%s %s", normalizedMethod, normalizedPath);
        return description == null || description.isBlank()
                ? operation
                : String.format("%s — %s", operation, description.trim());
    }

    public static String chain(HttpMethod firstMethod, String firstPath,
                               HttpMethod secondMethod, String secondPath,
                               String description) {
        String operation = String.format("%s %s → %s %s",
                firstMethod.value(), firstPath, secondMethod.value(), secondPath);
        return description == null || description.isBlank()
                ? operation
                : String.format("%s %s", operation, description);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
