package atf.api.reporting.api.contract;

import atf.api.reporting.api.ReportQueryParameters;
import atf.api.reporting.audit.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Audits JSON evidence already rendered into a report artifact.
 *
 * <p>This is intentionally renderer-neutral: it does not know Playwright,
 * YAML or the mock module. A catalog adapter supplies the expected routes and
 * the auditor compares parsed JSON trees, so whitespace and pretty-printing
 * cannot hide a response-body drift.</p>
 */
public final class ApiReportJsonEvidenceAuditor {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern TEXTAREA = Pattern.compile(
            "(?is)<textarea\\b[^>]*class\\s*=\\s*['\"]([^'\"]*\\bcode-block\\b[^'\"]*)['\"][^>]*>(.*?)</textarea>");
    private static final Pattern PRE = Pattern.compile(
            "(?is)<pre\\b[^>]*class\\s*=\\s*['\"]([^'\"]*\\bcode-block\\b[^'\"]*)['\"][^>]*>(.*?)</pre>");
    private final List<ApiReportRouteContract> routes;

    public ApiReportJsonEvidenceAuditor(ApiReportContractCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        routes = catalog.routes() == null ? List.of() : List.copyOf(catalog.routes());
    }

    public Result inspect(Path report) {
        Objects.requireNonNull(report, "report");
        try {
            return inspectHtml(Files.readString(report));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read API report for JSON evidence audit: " + report, error);
        }
    }

    public Result inspectHtml(String html) {
        List<JsonNode> rendered = renderedJsonDocuments(html == null ? "" : html);
        List<String> violations = new ArrayList<>();
        for (ApiReportRouteContract route : routes) {
            JsonNode expected;
            try {
                expected = parse(route.responseBody());
            } catch (RuntimeException error) {
                violations.add(route.key() + " -> configured response body is not valid JSON: "
                        + error.getMessage());
                continue;
            }
            if (rendered.stream().noneMatch(expected::equals)) {
                violations.add(route.key() + " -> configured response JSON was not rendered");
            }
        }
        return new Result(routes.size(), rendered.size(), violations);
    }

    /** Compares one response body with the route selected by method, path and query. */
    public List<String> compareResponse(String method, String url, String responseBody) {
        if (method == null || method.isBlank() || url == null || url.isBlank()) {
            return List.of("method and URL are required to select an API report contract");
        }
        URI actual;
        try {
            actual = URI.create(url.trim());
        } catch (IllegalArgumentException error) {
            return List.of("invalid API report URL: " + url);
        }
        ReportQueryParameters query = ReportQueryParameters.fromUri(actual);
        Optional<ApiReportRouteContract> route = routes.stream()
                .filter(candidate -> candidate.matches(method.trim(), actual.getPath()))
                .filter(candidate -> query.containsAll(candidate.queryParameters()))
                .sorted(Comparator.comparingInt(
                        (ApiReportRouteContract candidate) -> candidate.queryParameters().values().size())
                        .reversed())
                .findFirst();
        if (route.isEmpty()) {
            return List.of(method.trim() + " " + actual.getPath()
                    + " -> no matching API report contract");
        }
        try {
            JsonNode expected = parse(route.get().responseBody());
            JsonNode actualBody = parse(responseBody);
            return expected.equals(actualBody)
                    ? List.of()
                    : List.of(route.get().key() + " -> rendered response JSON does not match the catalog");
        } catch (RuntimeException error) {
            return List.of(route.get().key() + " -> rendered response JSON cannot be compared: "
                    + error.getMessage());
        }
    }

    private static List<JsonNode> renderedJsonDocuments(String html) {
        List<JsonNode> result = new ArrayList<>();
        collect(html, TEXTAREA, result);
        collect(html, PRE, result);
        return result;
    }

    private static void collect(String html, Pattern pattern, List<JsonNode> result) {
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String source = unescapeHtml(matcher.group(2)).trim();
            if (source.isBlank()) continue;
            try {
                result.add(parse(source));
            } catch (RuntimeException ignored) {
                // Other code blocks belong to another evidence channel.
            }
        }
    }

    private static JsonNode parse(String source) {
        try {
            if (source == null || source.isBlank()) {
                throw new IllegalArgumentException("Missing JSON bytes; confirmed absence requires the capture-aware contract auditor");
            }
            return JSON.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(source);
        } catch (IOException error) {
            throw new IllegalArgumentException(error.getMessage(), error);
        }
    }

    private static String unescapeHtml(String value) {
        return value.replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    public record Result(int configuredRoutes, int renderedJsonDocuments, List<String> violations) {
        public Result {
            violations = violations == null ? List.of() : List.copyOf(violations);
        }

        public boolean compliant() {
            return configuredRoutes > 0 && renderedJsonDocuments > 0 && violations.isEmpty();
        }

        public ValidationResult validation() {
            return ValidationResult.fromViolations("API JSON evidence", violations);
        }
    }
}
