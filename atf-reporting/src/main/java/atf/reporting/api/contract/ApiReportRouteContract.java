package atf.reporting.api.contract;

import atf.reporting.api.ReportQueryParameters;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Renderer-neutral expectation for one API exchange represented in a report. */
public record ApiReportRouteContract(
        String method,
        String path,
        int status,
        String responseBody,
        Map<String, String> responseHeaders,
        String authProfile,
        ReportQueryParameters queryParameters,
        Map<String, String> requiredHeaders,
        String requestBodyMode,
        String expectedRequestBody) {

    public ApiReportRouteContract(String method, String path, int status, String responseBody,
                                  Map<String, String> responseHeaders, String authProfile,
                                  Object queryParameters, Map<String, String> requiredHeaders,
                                  String requestBodyMode, String expectedRequestBody) {
        this(method, path, status, responseBody, responseHeaders, authProfile,
                ReportQueryParameters.from(queryParameters), requiredHeaders, requestBodyMode, expectedRequestBody);
    }

    public static final String ANY_METHOD = "*";
    public static final String ANY_PATH = "/*";
    public static final String ANY_AUTH_PROFILE = "*";

    public ApiReportRouteContract {
        method = normalizeMethod(method);
        path = normalizePath(path);
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("report route status must be between 100 and 599");
        }
        responseBody = responseBody == null ? "" : responseBody;
        responseHeaders = copy(responseHeaders);
        authProfile = authProfile == null || authProfile.isBlank()
                ? ANY_AUTH_PROFILE : authProfile.trim();
        queryParameters = queryParameters == null ? ReportQueryParameters.empty() : queryParameters;
        requiredHeaders = copy(requiredHeaders);
        requestBodyMode = ApiReportRequestBodyMode.from(requestBodyMode).value();
        expectedRequestBody = expectedRequestBody == null || expectedRequestBody.isBlank()
                ? null : expectedRequestBody;
    }

    public String key() {
        if (queryParameters.isEmpty()) {
            return method + " " + path;
        }
        String query = queryParameters.values().stream()
                .sorted(Comparator.comparing(parameter -> parameter.name()
                        + "=" + parameter.value()))
                .map(parameter -> parameter.name() + "=" + parameter.value())
                .collect(Collectors.joining("&"));
        return method + " " + path + "?" + query;
    }

    public boolean matches(String actualMethod, String actualPath) {
        return (ANY_METHOD.equals(method) || method.equalsIgnoreCase(actualMethod))
                && (ANY_PATH.equals(path) || path.equals(actualPath));
    }

    private static String normalizeMethod(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("report route method must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("report route path must not be blank");
        }
        String path = value.trim();
        return path.startsWith("/") ? path : "/" + path;
    }

    private static Map<String, String> copy(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(values));
    }
}
