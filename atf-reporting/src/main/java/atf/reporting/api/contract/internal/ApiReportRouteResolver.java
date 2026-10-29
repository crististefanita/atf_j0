package atf.reporting.api.contract.internal;

import atf.reporting.api.ReportQueryParameters;
import atf.reporting.api.contract.ApiReportRouteContract;
import atf.reporting.audit.ValidationResult;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves the report's METHOD/path identity against a catalog port. */
public final class ApiReportRouteResolver {
    private static final Pattern REQUEST_SIGNATURE = Pattern.compile(
            "^\\s*([A-Za-z]+)\\s+([^\\s]+)(?:\\s+—.*)?\\s*$");
    private final List<ApiReportRouteContract> routes;

    public ApiReportRouteResolver(List<ApiReportRouteContract> routes) {
        this.routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public Resolution resolve(String requestTitle, String requestUrl, int actualStatus) {
        Signature signature = signature(requestTitle, requestUrl);
        if (signature == null) {
            String message = requestTitle == null || requestTitle.isBlank()
                    ? "API request title is blank; cannot resolve report contract"
                    : "API request title does not expose METHOD /path: " + requestTitle;
            return new Resolution(Optional.empty(), ValidationResult.ofViolations(List.of(message)));
        }

        ReportQueryParameters query = ApiReportContractValueSupport.queryFromUrl(requestUrl);
        List<ApiReportRouteContract> candidates = routes.stream()
                .filter(route -> route.matches(signature.method(), signature.path()))
                .toList();
        List<ApiReportRouteContract> queryMatches = candidates.stream()
                .filter(route -> query.containsAll(route.queryParameters()))
                .toList();
        List<ApiReportRouteContract> source = queryMatches.isEmpty() ? candidates : queryMatches;
        Optional<ApiReportRouteContract> route = source.stream()
                .sorted((left, right) -> {
                    int status = Boolean.compare(right.status() == actualStatus, left.status() == actualStatus);
                    return status != 0 ? status
                            : Integer.compare(right.queryParameters().values().size(),
                            left.queryParameters().values().size());
                })
                .findFirst();
        if (route.isEmpty()) {
            return new Resolution(Optional.empty(), ValidationResult.ofViolations(List.of(
                    signature.method() + " " + signature.path()
                            + " has no response contract in the configured catalog")));
        }
        ApiReportRouteContract expected = route.get();
        ValidationResult queryValidation = ApiReportRequestMetadataValidator.validateQuery(expected, query);
        if (actualStatus != expected.status()) {
            return new Resolution(route, queryValidation.merge(ValidationResult.ofViolations(List.of(
                    expected.key() + " expected HTTP " + expected.status()
                            + " from the configured catalog but report rendered " + actualStatus))));
        }
        return new Resolution(route, queryValidation);
    }

    private static Signature signature(String title, String requestUrl) {
        if (title == null || title.isBlank()) return null;
        Matcher matcher = REQUEST_SIGNATURE.matcher(title.replaceAll("\\s+", " ").trim());
        if (!matcher.matches()) return null;
        String path = matcher.group(2);
        if (requestUrl != null && !requestUrl.isBlank()) {
            try {
                String renderedPath = URI.create(requestUrl.trim()).getPath();
                if (renderedPath != null && !renderedPath.isBlank()) path = renderedPath;
            } catch (IllegalArgumentException ignored) { }
        }
        return new Signature(matcher.group(1), path);
    }

    public record Resolution(Optional<ApiReportRouteContract> route, ValidationResult validation) {
        public Resolution {
            route = route == null ? Optional.empty() : route;
            validation = Objects.requireNonNull(validation, "validation");
        }
    }

    private record Signature(String method, String path) { }
}
