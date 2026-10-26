package atf.reporting.support;

import atf.core.config.source.ApiEnvironment;
import atf.core.http.response.ApiOperationLabel;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;

import java.net.URI;

/** Typed URL and operation vocabulary for reporting test fixtures. */
public final class ApiReportTestUrls {
    public static final URI EXAMPLE_BASE_URI = URI.create("https://example.test");
    public static final URI API_EXAMPLE_BASE_URI = URI.create("https://api.example.test");
    public static final URI MOCK_BASE_URI = URI.create("http://mock.atf.test");
    public static final URI CONFIGURED_BASE_URI = URI.create("https://configured.example");

    public static final String USERS = MockRoutePaths.USERS;
    public static final String POSTS = MockRoutePaths.POSTS;
    public static final String SEARCH = MockRoutePaths.SEARCH;
    /** Renderer-only paths deliberately kept separate from the application route catalog. */
    public static final String GENERIC_USERS = "/users";
    public static final String GENERIC_USER_ONE = GENERIC_USERS + "/1";
    public static final String GENERIC_CHECKOUT = "/checkout";
    public static final String GENERIC_SECURE = "/secure";
    public static final String GENERIC_OTHER = "/other";
    public static final String GENERIC_HEALTH = "/health";

    private ApiReportTestUrls() {
    }

    public static String example(String path) {
        return EXAMPLE_BASE_URI.resolve(path).toString();
    }

    public static String apiExample(String path) {
        return API_EXAMPLE_BASE_URI.resolve(path).toString();
    }

    public static String mock(String path) {
        return MOCK_BASE_URI.resolve(path).toString();
    }

    public static String atf(String path) {
        return ApiEnvironment.MOCK_BASE_URI.resolve(path).toString();
    }

    public static String operation(HttpMethod method, String path, String description) {
        return ApiOperationLabel.format(method, path, description);
    }

    public static String userById(String id) {
        return MockRoutePaths.userById(id);
    }

    public static String postById(String id) {
        return MockRoutePaths.postById(id);
    }

    public static String profileById(String id) {
        return MockRoutePaths.profileById(id);
    }
}
