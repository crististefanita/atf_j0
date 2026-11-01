package atf.mock;
import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.JsonBodyPolicy;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One deterministic method/path response owned by the ATF mock module. */
public record MockRoute(String method, String path, int status, String body,
                        Map<String, String> headers, Duration duration,
                        String authProfile, Map<String, String> query,
                        ApiQueryParameters queryParameters,
                        Map<String, String> requiredHeaders,
                        String requestBodyMode, String expectedRequestBody) {
    public static final String ANY_METHOD = "*";
    public static final String ANY_PATH = "/*";
    public static final String ANY_AUTH_PROFILE = "*";

    public MockRoute(String method, String path, int status, String body,
                     Map<String, String> headers, Duration duration) {
        this(method, path, status, body, headers, duration, ANY_AUTH_PROFILE, Map.of(), Map.of());
    }

    public MockRoute(String method, String path, int status, String body,
                     Map<String, String> headers, Duration duration,
                     String authProfile, Map<String, String> query) {
        this(method, path, status, body, headers, duration, authProfile, query, Map.of());
    }

    public MockRoute(String method, String path, int status, String body,
                     Map<String, String> headers, Duration duration,
                     String authProfile, Map<String, String> query,
                     Map<String, String> requiredHeaders) {
        this(method, path, status, body, headers, duration, authProfile, query,
                ApiQueryParameters.fromMap(query), requiredHeaders, MockRequestBodyMode.JSON.value(), null);
    }

    /** Compatibility constructor for callers that configure request-body semantics directly. */
    public MockRoute(String method, String path, int status, String body,
                     Map<String, String> headers, Duration duration,
                     String authProfile, Map<String, String> query,
                     Map<String, String> requiredHeaders, String requestBodyMode,
                     String expectedRequestBody) {
        this(method, path, status, body, headers, duration, authProfile, query,
                ApiQueryParameters.fromMap(query), requiredHeaders, requestBodyMode, expectedRequestBody);
    }

    public MockRoute(String method, String path, int status, String body,
                     Map<String, String> headers, Duration duration,
                     String authProfile, ApiQueryParameters queryParameters,
                     Map<String, String> requiredHeaders) {
        this(method, path, status, body, headers, duration, authProfile,
                queryParameters == null ? Map.of() : queryParameters.asMap(),
                queryParameters, requiredHeaders, MockRequestBodyMode.JSON.value(), null);
    }

    public MockRoute(String method, String path, int status, String body,
                     Map<String, String> headers, Duration duration,
                     String authProfile, ApiQueryParameters queryParameters,
                     Map<String, String> requiredHeaders, String requestBodyMode,
                     String expectedRequestBody) {
        this(method, path, status, body, headers, duration, authProfile,
                queryParameters == null ? Map.of() : queryParameters.asMap(),
                queryParameters, requiredHeaders, requestBodyMode, expectedRequestBody);
    }

    public MockRoute {
        method = normalizeMethod(method);
        path = normalizePath(path);
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("mock response status must be between 100 and 599");
        }
        body = body == null ? "" : body;
        if (JsonBodyPolicy.appliesTo(method)) {
            MockJsonDocument.require(body, "mock " + method + " " + path + " response body");
        }
        headers = headers == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(headers));
        duration = duration == null ? Duration.ofMillis(12) : duration;
        if (duration.isNegative()) throw new IllegalArgumentException("mock duration must not be negative");
        authProfile = authProfile == null || authProfile.isBlank()
                ? ANY_AUTH_PROFILE : authProfile.trim();
        query = query == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(query));
        queryParameters = queryParameters == null ? ApiQueryParameters.fromMap(query) : queryParameters;
        query = queryParameters.asMap();
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(requiredHeaders));
        requestBodyMode = MockRequestBodyMode.normalize(requestBodyMode);
        if (expectedRequestBody != null) MockJsonDocument.require(expectedRequestBody, "mock expected request body");
    }

    public String key() {
        if (queryParameters.isEmpty()) return method + " " + path;
        String queryIdentity = queryParameters.values().stream()
                .sorted(java.util.Comparator.comparing(ApiQueryParameter::name)
                        .thenComparing(ApiQueryParameter::value))
                .map(parameter -> parameter.name() + "=" + parameter.value())
                .collect(java.util.stream.Collectors.joining("&"));
        return method + " " + path + "?" + queryIdentity;
    }

    public boolean matches(ApiRequest request) {
        Objects.requireNonNull(request, "request");
        return (method.equals(ANY_METHOD) || method.equalsIgnoreCase(request.method()))
                && pathMatches(request)
                && requestBodyMatches(request)
                && authMatches(request)
                && queryMatches(request)
                && requiredHeaders.entrySet().stream().allMatch(entry ->
                request.headers().entrySet().stream().anyMatch(actual ->
                        actual.getKey().equalsIgnoreCase(entry.getKey())
                                && actual.getValue().equals(entry.getValue())));
    }

    private boolean pathMatches(ApiRequest request) {
        return path.equals(ANY_PATH) || path.equals(request.path());
    }

    private boolean authMatches(ApiRequest request) {
        return authProfile.equals(ANY_AUTH_PROFILE) || authProfile.equals(request.authProfile());
    }

    private boolean queryMatches(ApiRequest request) {
        return request.queryParameters().containsAll(queryParameters);
    }

    public ApiResponse response(String url) {
        return new ApiResponse(status, body, headers, duration, url);
    }

    private static String normalizeMethod(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("mock method must not be blank");
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private boolean requestBodyMatches(ApiRequest request) {
        if (!JsonBodyPolicy.appliesTo(method)) return true;
        var actual = MockJsonDocument.read(request.bodyText());
        boolean jsonContentType = hasJsonContentType(request);
        boolean matchesMode = switch (MockRequestBodyMode.from(requestBodyMode)) {
            case WRONG_CONTENT_TYPE -> actual.isPresent() && !jsonContentType;
            case JSON -> actual.isPresent() && jsonContentType;
        };
        if (!matchesMode) return false;
        return expectedRequestBody == null || MockJsonDocument.read(expectedRequestBody)
                .filter(actual.orElseThrow()::sameValue).isPresent();
    }

    private static boolean hasJsonContentType(ApiRequest request) {
        return request.headers().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(HttpHeader.CONTENT_TYPE.value()))
                .map(Map.Entry::getValue)
                .anyMatch(MediaTypes::isJson);
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("mock path must not be blank");
        String path = value.trim();
        return path.startsWith("/") ? path : "/" + path;
    }
}
