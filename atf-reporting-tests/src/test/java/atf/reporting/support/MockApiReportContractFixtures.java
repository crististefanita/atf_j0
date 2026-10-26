package atf.reporting.support;

import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.core.http.response.ApiOperationLabel;
import atf.mock.AtfMockConfiguration;
import atf.mock.MockRequestBodyMode;
import atf.mock.MockRoute;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.api.contract.ApiReportContractCatalog;
import atf.reporting.api.contract.ApiReportRouteContract;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Mock-backed, renderer-neutral inputs for the reporting contract tests. */
public final class MockApiReportContractFixtures {
    public static final String BASE_URL = ApiReportTestUrls.MOCK_BASE_URI.toString();
    public static final String DEMO_PATH = "/demo";
    public static final String SCENARIO = "demo";
    public static final String API_KEY_PROFILE = "api-key";
    public static final String BEARER_PROFILE = "bearer";
    public static final String CUSTOM_PROFILE = "custom";
    public static final String MODE_PARAMETER = "mode";
    public static final String VALIDATE_MODE = "validate";
    public static final String PAGE_PARAMETER = "page";
    public static final String FIRST_PAGE = "1";
    public static final String SECOND_PAGE = "2";
    public static final String REQUEST_BODY = "{\"name\":\"atf\"}";
    public static final String OK_BODY = "{\"ok\":true}";
    public static final String EMPTY_OBJECT = "{}";
    public static final String ACCEPTED_BODY = "{\"job\":{\"state\":\"accepted\"},\"items\":[1,2]}";
    public static final String REQUEST_TREE_BODY = "{\"request\":true}";
    public static final String BODY_MISMATCH = "{\"ok\":false}";
    public static final String JSON = MediaTypes.APPLICATION_JSON;
    public static final String PLAIN_TEXT = MediaTypes.TEXT_PLAIN;
    public static final Duration NO_DELAY = Duration.ZERO;
    public static final int OK_STATUS = 200;
    public static final int ACCEPTED_STATUS = 202;
    public static final int FALLBACK_STATUS = 500;

    private MockApiReportContractFixtures() { }

    public static AtfMockConfiguration configuration(MockRoute... routes) {
        return new AtfMockConfiguration(List.of(routes), fallbackRoute());
    }

    /** Test-only adapter proving that mock expectations can feed reporting without runtime coupling. */
    public static ApiReportContractCatalog catalog(MockRoute... routes) {
        return () -> List.of(routes).stream().map(MockApiReportContractFixtures::contract).toList();
    }

    private static ApiReportRouteContract contract(MockRoute route) {
        return new ApiReportRouteContract(route.method(), route.path(), route.status(), route.body(),
                route.headers(), route.authProfile(), route.queryParameters(), route.requiredHeaders(),
                route.requestBodyMode(), route.expectedRequestBody());
    }

    public static MockRoute acceptedPostRoute() {
        return new MockRoute(HttpMethod.POST.value(), DEMO_PATH, ACCEPTED_STATUS,
                OK_BODY, responseJsonHeaders(), NO_DELAY, API_KEY_PROFILE,
                Map.of(MODE_PARAMETER, VALIDATE_MODE),
                Map.of(HttpHeader.CONTENT_TYPE.value(), JSON,
                        HttpHeader.X_CLIENT.value(), "atf-java"),
                MockRequestBodyMode.JSON.value(), REQUEST_BODY);
    }

    public static MockRoute transportGetRoute() {
        return new MockRoute(HttpMethod.GET.value(), DEMO_PATH, OK_STATUS, OK_BODY,
                responseJsonHeaders(), NO_DELAY, BEARER_PROFILE,
                Map.of(PAGE_PARAMETER, SECOND_PAGE),
                Map.of(HttpHeader.ACCEPT.value(), JSON));
    }

    public static MockRoute responseTreePostRoute() {
        return new MockRoute(HttpMethod.POST.value(), DEMO_PATH, ACCEPTED_STATUS,
                ACCEPTED_BODY, jsonHeaders(), NO_DELAY);
    }

    public static MockRoute bodyDriftGetRoute() {
        return new MockRoute(HttpMethod.GET.value(), DEMO_PATH, OK_STATUS, OK_BODY,
                Map.of(), NO_DELAY);
    }

    public static MockRoute putRoute() {
        return new MockRoute(HttpMethod.PUT.value(), DEMO_PATH, OK_STATUS, OK_BODY,
                Map.of(), NO_DELAY);
    }

    public static ApiReportContractInput acceptedInput() {
        return new ApiReportContractInput(operation(HttpMethod.POST), acceptedUrl(), ACCEPTED_STATUS,
                REQUEST_BODY, OK_BODY, List.of(),
                Map.of("AUTH PROFILE", API_KEY_PROFILE,
                        "REQUEST HEADERS", "{Content-Type=" + JSON + ", X-Client=atf-java}"),
                Map.of("HTTP STATUS", String.valueOf(ACCEPTED_STATUS),
                        "CONTENT-TYPE", JSON,
                        "RESPONSE HEADERS", "[Content-Type=" + JSON + ", X-Mock-Scenario="
                                + SCENARIO + "]"),
                Map.of("response.status", String.valueOf(ACCEPTED_STATUS),
                        "response.url", acceptedUrl()));
    }

    public static ApiReportContractInput transportDriftInput() {
        return new ApiReportContractInput(operation(HttpMethod.GET), pagedUrl(), OK_STATUS,
                "null", OK_BODY, List.of(),
                Map.of("AUTH PROFILE", CUSTOM_PROFILE, "REQUEST HEADERS", "{Accept=" + PLAIN_TEXT + "}"),
                Map.of("HTTP STATUS", String.valueOf(OK_STATUS), "CONTENT-TYPE", PLAIN_TEXT,
                        "RESPONSE HEADERS", "[Content-Type=" + PLAIN_TEXT + "]"),
                Map.of("response.status", String.valueOf(FALLBACK_STATUS),
                        "response.url", url("/other")));
    }

    public static ApiReportContractInput responseTreeInput() {
        return ApiReportContractInput.of(operation(HttpMethod.POST), url(DEMO_PATH), ACCEPTED_STATUS,
                REQUEST_TREE_BODY, "{ \"items\" : [1, 2], \"job\" : { \"state\" : \"accepted\" } }");
    }

    public static ApiReportContractInput bodyDriftInput() {
        return ApiReportContractInput.of(operation(HttpMethod.GET), url(DEMO_PATH), FALLBACK_STATUS,
                "null", BODY_MISMATCH);
    }

    public static ApiReportContractInput putInput(String requestBody, String responseBody) {
        return ApiReportContractInput.of(operation(HttpMethod.PUT), url(DEMO_PATH), OK_STATUS,
                requestBody, responseBody);
    }

    public static ApiReportContractInput unresolvedInput() {
        return ApiReportContractInput.of(operation(HttpMethod.GET), url(DEMO_PATH), FALLBACK_STATUS,
                "null", EMPTY_OBJECT);
    }

    public static String operation(HttpMethod method) {
        return ApiOperationLabel.format(method, DEMO_PATH, null);
    }

    public static String url(String path) {
        return BASE_URL + path;
    }

    public static String acceptedUrl() {
        return url(DEMO_PATH) + "?" + MODE_PARAMETER + "=" + VALIDATE_MODE;
    }

    public static String pagedUrl() {
        return url(DEMO_PATH) + "?" + PAGE_PARAMETER + "=" + FIRST_PAGE;
    }

    public static MockRoute fallbackRoute() {
        return new MockRoute(MockRoute.ANY_METHOD, MockRoute.ANY_PATH, FALLBACK_STATUS,
                "{\"message\":\"fallback\"}", Map.of(), NO_DELAY);
    }

    private static Map<String, String> jsonHeaders() {
        return Map.of(HttpHeader.CONTENT_TYPE.value(), JSON);
    }

    private static Map<String, String> responseJsonHeaders() {
        return Map.of(HttpHeader.CONTENT_TYPE.value(), JSON,
                HttpHeader.X_MOCK_SCENARIO.value(), SCENARIO);
    }

    public enum ViolationSignal {
        AUTH_PROFILE("AUTH PROFILE"),
        QUERY("query page"),
        REQUEST_HEADER("request header"),
        RESPONSE_HEADER("response header"),
        EXECUTION_STATUS("execution evidence response.status"),
        NO_RESPONSE_CONTRACT("no response contract"),
        EXPECTED_STATUS("expected HTTP 200"),
        BODY_DRIFT("differs from configured catalog body"),
        REQUEST_JSON_BODY("non-empty JSON request body"),
        RESPONSE_JSON_BODY("non-empty JSON response body");

        private final String text;

        ViolationSignal(String text) {
            this.text = text;
        }

        public boolean occursIn(List<String> violations) {
            return violations.stream().anyMatch(violation -> violation.contains(text));
        }
    }
}
