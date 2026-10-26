package atf.mock;
import atf.testkit.AtfComponentTest;

import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.ApiRoute;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.mock.fixtures.MockTransportTestFixtures;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class AtfMockTransportTest {
    @Test
    void acceptsJsonRequestBodyAndReturnsTheConfiguredResponse() {
        var request = MockTransportTestFixtures.jsonRequest(HttpMethod.POST,
                MockTransportTestFixtures.USERS_PATH, MockTransportTestFixtures.USER_BODY);

        var response = AtfMockTransport.defaults()
                .send(request, MockTransportTestFixtures.url(MockTransportTestFixtures.USERS_PATH),
                        MockTransportTestFixtures.NO_AUTH_PROFILE)
                .join();

        assertAll(
                () -> assertEquals(401, response.status()),
                () -> assertEquals(MockTransportTestFixtures.JSON,
                        response.header(HttpHeader.CONTENT_TYPE.value())),
                () -> assertEquals(MockTransportTestFixtures.AUTH_REQUIRED_BODY, response.body()));
    }

    @Test
    void builtInFixturePreservesAcceptanceNegativeRoute() {
        var request = MockTransportTestFixtures.jsonRequest(HttpMethod.POST,
                MockTransportTestFixtures.USERS_PATH, MockTransportTestFixtures.USER_BODY);
        var response = AtfMockTransport.defaults()
                .send(request,
                        MockTransportTestFixtures.url(MockTransportTestFixtures.USERS_PATH),
                        MockTransportTestFixtures.NO_AUTH_PROFILE)
                .join();

        assertAll(
                () -> assertEquals(401, response.status()),
                () -> assertEquals(MockTransportTestFixtures.JSON,
                        response.header(HttpHeader.CONTENT_TYPE.value())),
                () -> assertEquals(MockTransportTestFixtures.AUTH_REQUIRED_BODY, response.body()),
                () -> assertEquals(MockTransportTestFixtures.url(MockTransportTestFixtures.USERS_PATH),
                        response.url()));
    }

    @Test
    void writeRoutesRejectEmptyOrNonJsonRequestsAndInvalidJsonResponses() throws Exception {
        var route = MockTransportTestFixtures.apiRoute(HttpMethod.POST,
                MockTransportTestFixtures.WRITE_PATH, 201, MockTransportTestFixtures.WRITE_BODY);
        var transport = new AtfMockTransport(new AtfMockConfiguration(
                MockTransportTestFixtures.routes(route),
                MockTransportTestFixtures.fallbackRoute(500, MockTransportTestFixtures.FALLBACK_BODY)));

        var empty = transport.send(MockTransportTestFixtures.emptyRequest(HttpMethod.POST,
                MockTransportTestFixtures.WRITE_PATH),
                MockTransportTestFixtures.url(MockTransportTestFixtures.WRITE_PATH),
                MockTransportTestFixtures.NO_AUTH_PROFILE).join();
        var invalid = MockTransportTestFixtures.jsonRequest(HttpMethod.POST,
                MockTransportTestFixtures.WRITE_PATH, MockTransportTestFixtures.INVALID_RESPONSE_BODY);
        var rejected = transport.send(invalid,
                MockTransportTestFixtures.url(MockTransportTestFixtures.WRITE_PATH),
                MockTransportTestFixtures.NO_AUTH_PROFILE).join();

        assertAll(
                () -> assertEquals(500, empty.status()),
                () -> assertEquals(500, rejected.status()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MockRoute(HttpMethod.PUT.value(),
                                MockTransportTestFixtures.BROKEN_RESPONSE_PATH, 200,
                                MockTransportTestFixtures.INVALID_RESPONSE_BODY, Map.of(),
                                Duration.ZERO)));
    }

    @Test
    void yamlRouteCatalogControlsStatusBodyHeadersAndDuration() throws Exception {
        Path config = Files.createTempFile("atf-mock-", ".yaml");
        try {
            Files.writeString(config, """
                    mock:
                      routes:
                        - method: GET
                          path: /demo
                          status: 202
                          duration: PT0.007S
                          headers:
                            Content-Type: application/custom+json
                          body: |-
                            {\"accepted\":true}
                      fallback:
                        status: 418
                        body: |-
                          {\"message\":\"not configured\"}
                    """);

            var transport = AtfMockTransport.fromConfiguration(config);
            var matched = transport.send(ApiRequest.of(HttpMethod.GET.value(),
                            MockTransportTestFixtures.DEMO_PATH),
                    MockTransportTestFixtures.url(MockTransportTestFixtures.DEMO_PATH),
                    MockTransportTestFixtures.NO_AUTH_PROFILE).join();
            var fallback = transport.send(ApiRequest.of(HttpMethod.GET.value(),
                            MockTransportTestFixtures.MISSING_PATH),
                    MockTransportTestFixtures.url(MockTransportTestFixtures.MISSING_PATH),
                    MockTransportTestFixtures.NO_AUTH_PROFILE).join();

            assertAll(
                    () -> assertEquals(202, matched.status()),
                    () -> assertEquals("{\"accepted\":true}", matched.body()),
                    () -> assertEquals(MockTransportTestFixtures.CUSTOM_JSON,
                            matched.header(HttpHeader.CONTENT_TYPE.value())),
                    () -> assertEquals(7, matched.duration().toMillis()),
                    () -> assertEquals(418, fallback.status()),
                    () -> assertTrue(fallback.body().contains("not configured")));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void yamlRouteCanRequireAuthProfileAndQueryParameters() throws Exception {
        Path config = Files.createTempFile("atf-mock-auth-query-", ".yaml");
        try {
            Files.writeString(config, """
                    mock:
                      routes:
                        - method: GET
                          path: /search
                          auth-profile: bearer
                          query:
                            page: "2"
                            include: details
                          status: 200
                          body: |-
                            {"results":[{"id":1,"attributes":{"active":true}}]}
                      fallback:
                        status: 418
                        body: '{"message":"unmatched"}'
                    """);

            var route = new ApiRoute("search", HttpMethod.GET.value(),
                    MockTransportTestFixtures.SEARCH_PATH, "search", "read",
                    MockTransportTestFixtures.BEARER_PROFILE);
            var request = route.request(Map.of(),
                    Map.of("page", "2", "include", "details"), Map.of());
            var matched = AtfMockTransport.fromConfiguration(config)
                    .send(request, MockTransportTestFixtures.url(
                            MockTransportTestFixtures.SEARCH_PATH + "?page=2&include=details"),
                    MockTransportTestFixtures.BEARER_PROFILE)
                    .join();

            assertAll(
                    () -> assertEquals(200, matched.status()),
                    () -> assertTrue(matched.body().contains("\"active\":true")),
                    () -> assertEquals(MockTransportTestFixtures.JSON,
                            matched.header(HttpHeader.CONTENT_TYPE.value())));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void yamlQueryArrayDistinguishesMissingEmptyAndDuplicateParameters() throws Exception {
        Path config = Files.createTempFile("atf-mock-query-shapes-", ".yaml");
        try {
            Files.writeString(config, """
                    mock:
                      routes:
                        - method: GET
                          path: /query/missing
                          query:
                            include: details
                          status: 200
                          body: '{"state":"missing-page"}'
                        - method: GET
                          path: /query/empty
                          query:
                            - name: page
                              value: ''
                          status: 400
                          body: '{"state":"empty-page"}'
                        - method: GET
                          path: /query/duplicate
                          query:
                            - name: tag
                              value: api
                            - name: tag
                              value: contract
                          status: 200
                          body: '{"state":"duplicate-tags"}'
                      fallback:
                        status: 418
                        body: '{"state":"fallback"}'
                    """);

            var transport = AtfMockTransport.fromConfiguration(config);
            var missing = new ApiRoute("missing", HttpMethod.GET.value(),
                    MockTransportTestFixtures.QUERY_MISSING_PATH, "query", "read",
                    MockTransportTestFixtures.NO_AUTH_PROFILE).request(Map.of(),
                    Map.of("include", "details"), Map.of());
            var empty = new ApiRoute("empty", HttpMethod.GET.value(),
                    MockTransportTestFixtures.QUERY_EMPTY_PATH, "query", "read",
                    MockTransportTestFixtures.NO_AUTH_PROFILE).request(Map.of(),
                    ApiQueryParameters.of(new ApiQueryParameter("page", "")), Map.of());
            var duplicate = new ApiRoute("duplicate", HttpMethod.GET.value(),
                    MockTransportTestFixtures.QUERY_DUPLICATE_PATH, "query", "read",
                    MockTransportTestFixtures.NO_AUTH_PROFILE).request(Map.of(),
                    ApiQueryParameters.of(new ApiQueryParameter("tag", "api"),
                            new ApiQueryParameter("tag", "contract")), Map.of());

            assertAll(
                    () -> assertEquals(200, transport.send(missing,
                            MockTransportTestFixtures.url(MockTransportTestFixtures.QUERY_MISSING_PATH + "?include=details"),
                            MockTransportTestFixtures.NO_AUTH_PROFILE).join().status()),
                    () -> assertEquals(400, transport.send(empty,
                            MockTransportTestFixtures.url(MockTransportTestFixtures.QUERY_EMPTY_PATH + "?page="),
                            MockTransportTestFixtures.NO_AUTH_PROFILE).join().status()),
                    () -> assertEquals(200, transport.send(duplicate,
                            MockTransportTestFixtures.url(MockTransportTestFixtures.QUERY_DUPLICATE_PATH + "?tag=api&tag=contract"),
                            MockTransportTestFixtures.NO_AUTH_PROFILE).join().status()),
                    () -> assertEquals(418, transport.send(
                            new ApiRoute("duplicate", HttpMethod.GET.value(),
                                    MockTransportTestFixtures.QUERY_DUPLICATE_PATH, "query", "read",
                                    MockTransportTestFixtures.NO_AUTH_PROFILE)
                                    .request(Map.of(), ApiQueryParameters.of(new ApiQueryParameter("tag", "api")), Map.of()),
                            MockTransportTestFixtures.url(MockTransportTestFixtures.QUERY_DUPLICATE_PATH + "?tag=api"),
                            MockTransportTestFixtures.NO_AUTH_PROFILE).join().status()));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void yamlRouteCanRequireCaseInsensitiveRequestHeadersAndReturnResponseHeaders() throws Exception {
        Path config = Files.createTempFile("atf-mock-request-headers-", ".yaml");
        try {
            Files.writeString(config, """
                    mock:
                      routes:
                        - method: GET
                          path: /headers
                          request-headers:
                            X-Correlation-Id: corr-001
                            Accept: application/json
                          status: 200
                          headers:
                            Content-Type: application/json
                            X-Request-Id: response-001
                            ETag: W/catalog-1
                          body: '{"ok":true}'
                      fallback:
                        status: 428
                        body: '{"message":"required headers missing"}'
                    """);

            var route = new ApiRoute("headers", HttpMethod.GET.value(),
                    MockTransportTestFixtures.HEADERS_PATH, "headers", "read",
                    MockTransportTestFixtures.NO_AUTH_PROFILE);
            var accepted = route.request(Map.of(), Map.of(),
                    Map.of("x-correlation-id", "corr-001", "accept", MockTransportTestFixtures.JSON));
            var rejected = route.request(Map.of(), Map.of(),
                    Map.of(HttpHeader.ACCEPT.value(), MockTransportTestFixtures.JSON));
            var transport = AtfMockTransport.fromConfiguration(config);
            var matched = transport.send(accepted,
                    MockTransportTestFixtures.url(MockTransportTestFixtures.HEADERS_PATH),
                    MockTransportTestFixtures.NO_AUTH_PROFILE).join();
            var fallback = transport.send(rejected,
                    MockTransportTestFixtures.url(MockTransportTestFixtures.HEADERS_PATH),
                    MockTransportTestFixtures.NO_AUTH_PROFILE).join();

            assertAll(
                    () -> assertEquals(200, matched.status()),
                    () -> assertEquals("response-001", matched.header("x-request-id")),
                    () -> assertEquals("W/catalog-1", matched.header("ETag")),
                    () -> assertEquals(428, fallback.status()));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void discoversMockOwnedCatalogWithoutReadingGenericAtfYaml() throws Exception {
        Path root = Files.createTempDirectory("atf-mock-discovery-");
        Path nested = Files.createDirectories(root.resolve("nested").resolve("tests"));
        try {
            Files.writeString(root.resolve("atf.yaml"), """
                    api:
                      baseUri: https://example.test
                    reporting:
                      extent:
                        enabled: true
                    """);
            Path mockConfig = root.resolve("atf-mock.yaml");
            Files.writeString(mockConfig, """
                    mock:
                      routes:
                        - method: GET
                          path: /owned
                          status: 204
                          body: ''
                    """);

            assertEquals(mockConfig.toAbsolutePath().normalize(),
                    AtfMockConfigurationLoader.discover(nested));
            var response = AtfMockTransport.fromConfiguration(mockConfig)
                    .send(ApiRequest.of(HttpMethod.GET.value(), MockTransportTestFixtures.OWNED_PATH),
                            MockTransportTestFixtures.url(MockTransportTestFixtures.OWNED_PATH),
                            MockTransportTestFixtures.NO_AUTH_PROFILE)
                    .join();
            assertEquals(204, response.status());
        } finally {
            Files.walk(root)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException error) {
                            throw new RuntimeException(error);
                        }
                    });
        }
    }

    @Test
    void rejectsGenericAtfYamlWhenExplicitlySelectedAsMockCatalog() throws Exception {
        Path config = Files.createTempFile("atf-generic-", ".yaml");
        try {
            Files.writeString(config, """
                    api:
                      baseUri: https://example.test
                    reporting:
                      extent:
                        enabled: true
                    """);

            var error = assertThrows(
                    IllegalStateException.class,
                    () -> AtfMockConfigurationLoader.load(config));
            assertTrue(error.getMessage().contains("root 'mock' section"));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void matchesNegativeRequestBodyModesWithoutWeakeningPostPutJsonContracts() {
        var invalidJsonOutcome = MockTransportTestFixtures.jsonContractRoute(HttpMethod.POST,
                "/invalid", 400, "{\"ok\":false}");
        var missingBodyOutcome = MockTransportTestFixtures.jsonContractRoute(HttpMethod.POST,
                MockTransportTestFixtures.MISSING_PATH, 400, "{\"ok\":false}");

        var invalidOutcome = MockTransportTestFixtures.jsonRequest(HttpMethod.POST,
                "/invalid", "{\"rawBody\":\"not-json\"}");
        var missingOutcome = MockTransportTestFixtures.jsonRequest(HttpMethod.POST,
                MockTransportTestFixtures.MISSING_PATH, "{\"body\":null}");

        assertTrue(invalidJsonOutcome.matches(invalidOutcome));
        assertTrue(missingBodyOutcome.matches(missingOutcome));
        assertThrows(IllegalArgumentException.class, () -> new MockRoute(
                HttpMethod.PUT.value(), MockTransportTestFixtures.BAD_RESPONSE_PATH, 200,
                MockTransportTestFixtures.INVALID_RESPONSE_BODY, Map.of(), Duration.ZERO));
    }
}
