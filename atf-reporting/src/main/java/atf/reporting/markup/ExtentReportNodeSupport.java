package atf.reporting.markup;
import atf.reporting.contract.AtfReportStatus;

import atf.reporting.configuration.ReportingSettings;


/** Small, renderer-local policies shared by the Extent tree scopes. */
public final class ExtentReportNodeSupport {
    private static final String TRUNCATION_MARKER = "… [truncated; showing first %d of %d chars]";

    private ExtentReportNodeSupport() { }

    public static String renderRequestTitle(String title, String method, String path) {
        String route = ((method == null ? "" : method.trim()) + " "
                + (path == null ? "" : path.trim())).trim();
        String normalizedTitle = title == null ? "" : title.trim();
        // TRACE is an internal evidence node, not an HTTP exchange. Its path
        // is the correlation target and must not become part of the visible
        // Gherkin/trace title.
        if ("TRACE".equalsIgnoreCase(method)) {
            return normalizedTitle.isBlank() ? "TRACE" : normalizedTitle;
        }
        if (route.isBlank()) return normalizedTitle.isBlank() ? "API request" : normalizedTitle;
        if (normalizedTitle.isBlank() || normalizedTitle.equals(route)) return route;
        if (normalizedTitle.startsWith(route + " — ")
                || normalizedTitle.startsWith(route + " · ")) {
            return normalizedTitle;
        }
        return route + " — " + normalizedTitle;
    }

    public static String normalizeDetailName(String name) {
        if (name == null || name.isBlank()) return "DETAIL";
        return switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "response.duration.ms", "duration", "duration.ms" -> "response duration";
            case "response.status", "http.status", "status" -> "HTTP STATUS";
            case "route", "request.route" -> "ROUTE TEMPLATE";
            default -> name;
        };
    }

    public static String display(String value, ReportingSettings settings) {
        String safe = value == null ? "" : value;
        int limit = settings.assertionMaxDisplayCharacters();
        return truncate(safe, limit);
    }

    public static String displayBody(String value, ReportingSettings settings) {
        String safe = value == null ? "" : value;
        if (settings.bodyMaxDisplayCharacters() == ReportingSettings.DISABLE_BODY_DISPLAY_LIMIT) {
            return safe;
        }
        return truncate(safe, settings.bodyMaxDisplayCharacters());
    }

    private static String truncate(String value, int limit) {
        return value.length() <= limit
                ? value
                : value.substring(0, limit) + String.format(TRUNCATION_MARKER, limit, value.length());
    }

    public static String resultMarker(AtfReportStatus status) {
        return AtfReportStatus.orInfo(status).marker();
    }
}
