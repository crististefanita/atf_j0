package atf.mock.json;

import atf.core.http.request.ApiRequest;
import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockTransport;
import atf.mock.MockRequestBodyMode;
import atf.mock.MockRoute;
import atf.testkit.AtfComponentTest;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class MockJsonFixtureComponentTest {
    @ParameterizedTest @EmptySource
    @ValueSource(strings = {" ", "not-json", "{", "{} trailing", "{} {}", "[] false", "null true",
            "42 true", "\"text\" extra", "{\"x\":1,\"x\":2}", "{\"x\":{\"v\":1,\"v\":2}}"})
    void suppliedExpectedMustBeOneUnambiguousDocument(String expected) {
        for (String method : List.of("POST", "PUT", "GET"))
            assertThrows(IllegalArgumentException.class, () -> route(method, "{}", expected, "json"), expected);
    }

    @ParameterizedTest @NullSource @EmptySource
    @ValueSource(strings = {" ", "not-json", "{", "{} trailing", "{} {}", "[] false", "null true",
            "42 true", "\"text\" extra", "{\"x\":1,\"x\":2}", "{\"x\":{\"v\":1,\"v\":2}}"})
    void writeResponseMustBeOneUnambiguousDocument(String body) {
        for (String method : List.of("POST", "PUT")) for (int status : List.of(201, 400))
            assertThrows(IllegalArgumentException.class, () -> new MockRoute(method, "/fixture", status,
                    body, Map.of(), Duration.ZERO), body);
    }

    @ParameterizedTest @NullSource @EmptySource
    @ValueSource(strings = {" ", "not-json", "{", "{} trailing", "{} {}", "[] false", "null true",
            "42 true", "\"text\" extra", "{\"x\":1,\"x\":2}", "{\"x\":{\"v\":1,\"v\":2}}"})
    void malformedRequestCannotMatchEitherBodyMode(String body) {
        for (String method : List.of("POST", "PUT")) for (var mode : MockRequestBodyMode.values()) {
            String type = mode == MockRequestBodyMode.JSON ? "application/json" : "text/plain";
            assertFalse(route(method, "{}", null, mode.value()).matches(request(method, body, type)), body);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "null", "true", "42", "\"\"", "{\"a\":1,\"b\":[true,null]}"})
    void validJsonRootsRemainDistinctFromAbsentExpected(String json) {
        var route = route("POST", json, json, "json");
        assertTrue(route.matches(request("POST", json, "application/json")));
        assertEquals(json, route.response("/fixture").body());
        assertTrue(route("POST", "{}", null, "json").matches(request("POST", json, "application/json")));
    }

    @Test void semanticMatchingAndRawResponseBytesRemainStable() {
        String expected = "{\"a\":1,\"b\":[true,null]}";
        String actual = " { \"b\" : [true, null], \"a\" : 1 } ";
        String response = " { \"accepted\": true }\n";
        var route = route("POST", response, expected, "json");
        assertTrue(route.matches(request("POST", actual, "application/problem+json; charset=utf-8")));
        assertFalse(route.matches(request("POST", "{\"a\":\"1\",\"b\":[true,null]}", "application/json")));
        assertFalse(route.matches(request("POST", "{\"a\":1,\"b\":[null,true]}", "application/json")));
        var fallback = new MockRoute("*", "/*", 499, "fallback", Map.of(), Duration.ZERO);
        var transport = new AtfMockTransport(new AtfMockConfiguration(List.of(route), fallback));
        var result = transport.send(request("POST", actual, "application/json"), "https://mock.invalid/fixture", "none").join();
        assertEquals(201, result.status());
        assertEquals(response, result.body());
        assertEquals("https://mock.invalid/fixture", result.url());
        assertEquals(499, transport.send(request("POST", expected + " trailing", "application/json"), "/fixture", "none").join().status());
    }

    @Test void intentionalNegativeFixturesAndReadBodiesRemainSupported() {
        var wrongType = route("PUT", "{\"message\":\"wrong type\"}", "{}", "wrong-content-type");
        assertTrue(wrongType.matches(request("PUT", "{}", "text/plain; profile=\"application/json\"")));
        assertFalse(wrongType.matches(request("PUT", "{}", "application/json")));
        assertFalse(wrongType.matches(request("PUT", "{} trailing", "text/plain")));
        assertTrue(route("PUT", "{\"ok\":false}", "{\"body\":null}", "json")
                .matches(request("PUT", "{\"body\":null}", "application/json")));
        assertEquals("not-json", route("GET", "not-json", null, "json").response("/fixture").body());
        assertEquals("", route("GET", null, null, "json").response("/fixture").body());
    }

    @Test void parserCauseRemainsAvailableWithoutEchoingPayloadInTheTopLevelMessage() {
        var error = assertThrows(IllegalArgumentException.class, () -> route("POST", "{} {}", null, "json"));
        assertInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class, error.getCause());
        assertTrue(error.getMessage().contains("response body"));
        assertFalse(error.getMessage().contains("{} {}"));
    }

    @Test void matchingUsesValuesBeyondTheReportingPreview() {
        String prefix = "audit-".repeat(45);
        var route = route("POST", "{}", "{\"value\":\"" + prefix + "A\"}", "json");
        assertFalse(route.matches(request("POST", "{\"value\":\"" + prefix + "B\"}", "application/json")));
    }

    @Test void concurrentMatchingDoesNotShareParsedRequestState() {
        var route = route("POST", "{}", "{\"id\":1}", "json");
        java.util.stream.IntStream.range(0, 50).parallel().forEach(index -> {
            boolean expected = index % 2 == 0;
            String body = expected ? "{\"id\":1}" : "{\"id\":2}";
            assertEquals(expected, route.matches(request("POST", body, "application/json")));
        });
    }

    private static MockRoute route(String method, String response, String expected, String mode) {
        return new MockRoute(method, "/fixture", 201, response, Map.of(), Duration.ZERO,
                "*", Map.of(), Map.of(), mode, expected);
    }
    private static ApiRequest request(String method, String body, String type) {
        String text = body == null ? "" : body;
        return new ApiRequest(HttpRequest.BodyPublishers.ofString(text), method, "/fixture",
                Map.of(), Map.of("Content-Type", type), "none", null, text);
    }
}
