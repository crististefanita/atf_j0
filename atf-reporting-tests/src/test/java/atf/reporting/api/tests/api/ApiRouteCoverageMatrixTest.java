package atf.reporting.api.tests.api;
import atf.testkit.AtfComponentTest;
import atf.reporting.api.tests.support.ApiTestResources;


import atf.mock.MockRoute;
import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reconciles the mock catalog with the executable API-test ownership map.
 * The test is intentionally data-driven: adding a YAML route without adding
 * an owner/evidence declaration fails here instead of being found by hand in
 * a rendered report.
 */
@AtfComponentTest
class ApiRouteCoverageMatrixTest {
    private static final String USERS_OWNER = "atf.reporting.api.tests.api.users.UsersApiTests";
    private static final String POSTS_OWNER = "atf.reporting.api.tests.api.posts.PostsApiTests";
    private static final String REQUESTS_OWNER = "atf.reporting.api.tests.api.requests.RequestVariantsApiTests";
    private static final String HEADERS_OWNER = "atf.reporting.api.tests.api.headers.ApiHeadersAndEdgeCasesTests";
    private static final String NEGATIVE_OWNER = "atf.reporting.api.tests.api.negative.ApiNegativeMatrixTests";
    private static final Map<String, String> OWNERS = owners();

    @Test
    void everyMockRouteHasOneExecutableOwnerAndNoOwnerPointsOutsideTheMock() {
        var mock = ApiTestResources.mockConfiguration();
        Set<String> mockKeys = mock.routes().stream().map(MockRoute::key).collect(java.util.stream.Collectors.toSet());
        Set<String> declaredKeys = OWNERS.keySet();
        List<String> missingDeclarations = mockKeys.stream().filter(key -> !declaredKeys.contains(key)).sorted().toList();
        List<String> staleDeclarations = declaredKeys.stream().filter(key -> !mockKeys.contains(key)).sorted().toList();
        List<String> missingOwners = OWNERS.entrySet().stream()
                .filter(entry -> !classExists(entry.getValue()))
                .map(entry -> entry.getKey() + " -> " + entry.getValue()).sorted().toList();

        assertAll(
                () -> assertEquals(mockKeys.size(), mock.routes().size(), "mock route keys must be unique"),
                () -> assertTrue(missingDeclarations.isEmpty(), () -> "mock routes without API evidence owner: " + missingDeclarations),
                () -> assertTrue(staleDeclarations.isEmpty(), () -> "API evidence declarations without mock route: " + staleDeclarations),
                () -> assertTrue(missingOwners.isEmpty(), () -> "API owners do not exist: " + missingOwners));
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException error) {
            return false;
        }
    }

    private static Map<String, String> owners() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(key(HttpMethod.GET, MockRoutePaths.USERS), USERS_OWNER);
        values.put(key(HttpMethod.GET, resolve(MockRoutePaths.USER_BY_ID, "id", "1")), USERS_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.USERS), USERS_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS), POSTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.POSTS), POSTS_OWNER);
        values.put(key(HttpMethod.GET, resolve(MockRoutePaths.POST_BY_ID, "id", "101")), POSTS_OWNER);
        values.put(key(HttpMethod.GET, resolve(MockRoutePaths.POST_BY_ID, "id", "102")), POSTS_OWNER);
        values.put(key(HttpMethod.GET, resolve(MockRoutePaths.USER_POSTS, "userId", "1")), POSTS_OWNER);
        values.put(key(HttpMethod.GET, resolve(MockRoutePaths.POST_BY_ID, "id", "999999")), POSTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.SEARCH, Map.of("include", "details", "page", "2")), REQUESTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.SEARCH, Map.of("include", "details", "page", "invalid")), REQUESTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.SEARCH, Map.of("include", "details")), REQUESTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.SEARCH, Map.of("include", "details", "page", "")), REQUESTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.SEARCH,
                ApiQueryParameters.of(new ApiQueryParameter("tag", "owner"), new ApiQueryParameter("tag", "quality"))),
                REQUESTS_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.BULK_IMPORT, Map.of("mode", "validate")), REQUESTS_OWNER);
        values.put(key(HttpMethod.PUT, resolve(MockRoutePaths.PROFILE_BY_ID, "id", "1"), Map.of("expand", "permissions")), REQUESTS_OWNER);
        values.put(key(HttpMethod.GET, resolve(MockRoutePaths.PROFILE_BY_ID, "id", "1")), REQUESTS_OWNER);
        values.put(key(HttpMethod.PATCH, MockRoutePaths.PREFERENCES, Map.of("locale", "ro-RO")), REQUESTS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.CATALOG, Map.of("page", "3", "pageSize", "2")), HEADERS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.CATALOG_EXPORT, Map.of("format", "csv")), HEADERS_OWNER);
        values.put(key(HttpMethod.GET, MockRoutePaths.CATALOG_ITEM_42, Map.of("include", "summary")), HEADERS_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_INVALID_JSON), NEGATIVE_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_MISSING_BODY), NEGATIVE_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_WRONG_CONTENT_TYPE), NEGATIVE_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_MISSING_TITLE), NEGATIVE_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_CONFLICT), NEGATIVE_OWNER);
        values.put(key(HttpMethod.PUT, MockRoutePaths.PROFILE_FORBIDDEN), NEGATIVE_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_UNKNOWN_AUTH), NEGATIVE_OWNER);
        values.put(key(HttpMethod.POST, MockRoutePaths.POSTS_SERVER_ERROR), NEGATIVE_OWNER);
        return Map.copyOf(values);
    }

    private static String key(HttpMethod method, String path) {
        return key(method, path, ApiQueryParameters.empty());
    }

    private static String key(HttpMethod method, String path, Map<String, String> query) {
        return key(method, path, ApiQueryParameters.fromMap(query));
    }

    private static String key(HttpMethod method, String path, ApiQueryParameters query) {
        return new MockRoute(method.value(), path, 200, "{}", Map.of(), Duration.ZERO,
                "*", query, Map.of()).key();
    }

    private static String resolve(String template, String token, String value) {
        return template.replace("{" + token + "}", value);
    }
}
