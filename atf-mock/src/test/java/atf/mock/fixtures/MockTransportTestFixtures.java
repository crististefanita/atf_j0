package atf.mock.fixtures;
import atf.core.http.request.ApiRequest;
import atf.core.config.source.ApiEnvironment;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.mock.MockRoute;
import atf.mock.MockRoutePaths;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Test-only vocabulary for deterministic mock transport contracts. */
public final class MockTransportTestFixtures {
    public static final String BASE_URL = ApiEnvironment.MOCK_BASE_URI.toString();
    public static final String NO_AUTH_PROFILE = "none";
    public static final String BEARER_PROFILE = "bearer";
    public static final String JSON = MediaTypes.APPLICATION_JSON;
    public static final String CUSTOM_JSON = "application/custom+json";
    public static final String NO_DELAY = "PT0S";

    public static final String USERS_PATH = MockRoutePaths.USERS;
    public static final String WRITE_PATH = "/write";
    public static final String DEMO_PATH = "/demo";
    public static final String SEARCH_PATH = "/search";
    public static final String QUERY_MISSING_PATH = "/query/missing";
    public static final String QUERY_EMPTY_PATH = "/query/empty";
    public static final String QUERY_DUPLICATE_PATH = "/query/duplicate";
    public static final String HEADERS_PATH = "/headers";
    public static final String OWNED_PATH = "/owned";
    public static final String MISSING_PATH = "/missing";
    public static final String BROKEN_RESPONSE_PATH = "/broken";
    public static final String BAD_RESPONSE_PATH = "/bad-response";
    public static final String EMPTY_JSON_BODY = "{}";
    public static final String PAGE_QUERY = "page";
    public static final String INCLUDE_QUERY = "include";
    public static final String DETAILS_QUERY_VALUE = "details";
    public static final String SECOND_PAGE_VALUE = "2";
    public static final String INVALID_PAGE_VALUE = "invalid";

    public static final String USER_BODY = "{\"name\":\"ATF user\",\"email\":\"atf@example.test\"}";
    public static final String AUTH_REQUIRED_BODY = "{\"message\":\"Authentication required\"}";
    public static final String WRITE_BODY = "{\"ok\":true}";
    public static final String FALLBACK_BODY = "{\"message\":\"fallback\"}";
    public static final String CUSTOM_RESPONSE_BODY = "{\"accepted\":true}";
    public static final String SEARCH_RESPONSE_BODY = "{\"results\":[{\"id\":1,\"attributes\":{\"active\":true}}]}";
    public static final String INVALID_RESPONSE_BODY = "not-json";

    private MockTransportTestFixtures() { }

    public static String url(String path) {
        return BASE_URL + path;
    }

    public static Map<String, String> jsonHeaders() {
        return Map.of(HttpHeader.CONTENT_TYPE.value(), JSON);
    }

    public static ApiRequest jsonRequest(HttpMethod method, String path, String body) {
        return new ApiRequest(HttpRequest.BodyPublishers.ofString(body), method.value(), path,
                Map.of(), jsonHeaders(), NO_AUTH_PROFILE, null, body);
    }

    public static ApiRequest emptyRequest(HttpMethod method, String path) {
        return ApiRequest.of(method.value(), path);
    }

    public static MockRoute apiRoute(HttpMethod method, String path, int status, String body) {
        return new MockRoute(method.value(), path, status, body, jsonHeaders(), Duration.ZERO);
    }

    public static MockRoute responseRoute(HttpMethod method, String path, int status, String body,
                                          Map<String, String> headers) {
        return new MockRoute(method.value(), path, status, body, headers, Duration.ZERO);
    }

    public static MockRoute fallbackRoute(int status, String body) {
        return new MockRoute(MockRoute.ANY_METHOD, MockRoute.ANY_PATH, status, body,
                Map.of(), Duration.ZERO);
    }

    public static MockRoute jsonContractRoute(HttpMethod method, String path, int status, String body) {
        return new MockRoute(method.value(), path, status, body, jsonHeaders(), Duration.ZERO,
                MockRoute.ANY_AUTH_PROFILE, Map.of(), Map.of(), "json", null);
    }

    public static List<MockRoute> routes(MockRoute... routes) {
        return List.of(routes);
    }
}
