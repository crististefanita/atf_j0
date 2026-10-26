package atf.reporting.support;

import atf.core.http.request.ApiRoute;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.JsonBodyPolicy;
import atf.core.http.transport.ApiTransportProvenance;
import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockTransport;
import atf.mock.MockRoute;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.api.contract.ApiReportEvidenceField;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Test-only composition root for the reporting integration slice.
 *
 * <p>It deliberately duplicates no production rule. It only executes one
 * configured mock exchange and projects the resulting wire evidence into the
 * renderer-neutral input consumed by the reporting contract auditor. This
 * keeps the reporting module independently testable without moving product
 * scenarios or browser code into it.</p>
 */
public final class MockApiReportExecution {
    private MockApiReportExecution() { }

    public static Exchange execute(AtfMockConfiguration configuration,
                                   MockRoute route,
                                   String requestBody,
                                   String baseUri) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(route, "route");
        String normalizedBaseUri = normalizeBaseUri(baseUri);
        ApiRoute semanticRoute = new ApiRoute(
                "mock-" + route.method() + "-" + route.path(),
                route.method(), route.path(), "mock", route.key(), route.authProfile());
        ApiRequest request = JsonBodyPolicy.appliesTo(route.method())
                ? semanticRoute.requestBody(requestBody, Map.of(), route.query(), route.requiredHeaders())
                : semanticRoute.request(Map.of(), route.query(), route.requiredHeaders());
        String url = url(normalizedBaseUri, request);
        ApiResponse response = new AtfMockTransport(configuration)
                .send(request, url, request.authProfile()).join();
        ApiExecutionResult execution = new ApiExecutionResult(
                response, 1, response.duration(), java.util.List.of(), java.util.List.of(),
                request, ApiTransportProvenance.ACCEPTANCE_MOCK);
        return new Exchange(request, execution);
    }

    public record Exchange(ApiRequest request, ApiExecutionResult execution) {
        public Exchange {
            request = Objects.requireNonNull(request, "request");
            execution = Objects.requireNonNull(execution, "execution");
        }

        public ApiReportContractInput contractInput() {
            ApiResponse response = execution.response();
            return new ApiReportContractInput(
                    request.method() + " " + request.path(),
                    response.url(),
                    response.status(),
                    request.bodyText(),
                    response.body(),
                    java.util.List.of(),
                    Map.of(
                            ApiReportEvidenceField.AUTH_PROFILE.key(), request.authProfile(),
                            ApiReportEvidenceField.REQUEST_HEADERS.key(), format(request.headers())),
                    Map.of(
                            ApiReportEvidenceField.HTTP_STATUS.key(), Integer.toString(response.status()),
                            ApiReportEvidenceField.CONTENT_TYPE.key(),
                            String.valueOf(response.header(HttpHeader.CONTENT_TYPE.value())),
                            ApiReportEvidenceField.RESPONSE_HEADERS.key(), format(response.headers())),
                    Map.of(
                            ApiReportEvidenceField.RESPONSE_STATUS.key(), Integer.toString(response.status()),
                            ApiReportEvidenceField.RESPONSE_URL.key(), response.url()));
        }

        public String requestBody() {
            return request.bodyText();
        }

        public String responseBody() {
            return execution.response().body();
        }
    }

    private static String url(String baseUri, ApiRequest request) {
        String query = request.queryParameters().values().stream()
                .map(parameter -> parameter.name() + "=" + parameter.value())
                .collect(Collectors.joining("&"));
        return query.isBlank() ? baseUri + request.path() : baseUri + request.path() + "?" + query;
    }

    private static String normalizeBaseUri(String baseUri) {
        String value = Objects.requireNonNull(baseUri, "baseUri").trim();
        if (value.isBlank()) throw new IllegalArgumentException("baseUri must not be blank");
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String format(Map<String, String> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
