package atf.reporting.api.tests.api.headers;

import atf.core.http.request.ApiRequest;
import atf.core.http.request.ApiRoute;
import atf.core.http.auth.AuthRequirement;
import atf.core.http.auth.AuthType;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.core.http.response.ApiOperationLabel;
import atf.mock.MockRoutePaths;
import atf.validation.contract.ResponseContract;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Request shapes for transport headers, caching and server-side limits. */
final class ApiHeaderRoutes {
    private static final ApiRoute CATALOG_PAGE = route(
            "catalog-page", HttpMethod.GET.value(), MockRoutePaths.CATALOG, "read", "bearer", AuthType.BEARER,
            Set.of("api", "headers", "pagination", "response-headers"));
    private static final ApiRoute EXPORT = route(
            "catalog-export", HttpMethod.GET.value(), MockRoutePaths.CATALOG_EXPORT, "read", "api-key", AuthType.API_KEY,
            Set.of("api", "headers", "rate-limit", "negative"));
    private static final ApiRoute NOT_MODIFIED = route(
            "catalog-cache", HttpMethod.GET.value(), MockRoutePaths.CATALOG_ITEM_42, "read", "custom", AuthType.CUSTOM,
            Set.of("api", "headers", "cache", "conditional"));

    private ApiHeaderRoutes() { }

    static ApiRequest catalogPageRequest() {
        return CATALOG_PAGE.request(Map.of(), Map.of("page", "3", "pageSize", "2"),
                Map.of(HttpHeader.ACCEPT.value(), MediaTypes.APPLICATION_JSON,
                        HttpHeader.X_CORRELATION_ID.value(), "corr-headers-001"));
    }

    static ApiRequest exportRequest() {
        return EXPORT.request(Map.of(), Map.of("format", "csv"),
                Map.of(HttpHeader.ACCEPT.value(), MediaTypes.TEXT_CSV,
                        HttpHeader.X_CLIENT_VERSION.value(), "2.4"));
    }

    static ApiRequest notModifiedRequest() {
        return NOT_MODIFIED.request(Map.of(), Map.of("include", "summary"),
                Map.of(HttpHeader.ACCEPT.value(), MediaTypes.APPLICATION_JSON,
                        HttpHeader.IF_NONE_MATCH.value(), "W/catalog-42"));
    }

    static String catalogPageTitle() {
        return ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.CATALOG, "request + response headers");
    }

    static String exportTitle() {
        return ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.CATALOG_EXPORT, "rate limit response");
    }

    static String notModifiedTitle() {
        return ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.CATALOG_ITEM_42, "conditional 304");
    }

    static List<String> catalogPageTags() {
        return List.of("api", "headers", "pagination", "response-headers");
    }

    static List<String> exportTags() {
        return List.of("api", "headers", "rate-limit", "negative");
    }

    static List<String> notModifiedTags() {
        return List.of("api", "headers", "cache", "conditional");
    }

    static ResponseContract catalogPageContract() {
        return new ResponseContract()
                .status(200)
                .contentTypeContains(MediaTypes.APPLICATION_JSON)
                .headerPresent(HttpHeader.X_REQUEST_ID.value())
                .headerPresent(HttpHeader.X_RATE_LIMIT_REMAINING.value())
                .headerPresent(HttpHeader.ETAG.value())
                .jsonPathEquals("$.page", "3")
                .jsonPathExists("$.links.self");
    }

    static ResponseContract exportContract() {
        return new ResponseContract()
                .status(429)
                .contentTypeContains(MediaTypes.APPLICATION_JSON)
                .headerPresent(HttpHeader.RETRY_AFTER.value())
                .headerPresent(HttpHeader.X_RATE_LIMIT_REMAINING.value())
                .jsonPathType("$.error", "object")
                .jsonPathEquals("$.error.code", "RATE_LIMITED")
                .bodyContains("Export quota exhausted");
    }

    static ResponseContract notModifiedContract() {
        return new ResponseContract()
                .status(304)
                .headerPresent(HttpHeader.ETAG.value())
                .headerPresent(HttpHeader.CACHE_CONTROL.value());
    }

    private static ApiRoute route(String name, String method, String path, String operation,
                                  String authProfile, AuthType authType, Set<String> tags) {
        return new ApiRoute(name, method, path, "headers", operation, authProfile,
                authType, AuthRequirement.REQUIRED, "required", tags);
    }
}
