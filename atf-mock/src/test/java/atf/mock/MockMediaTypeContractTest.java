package atf.mock;

import atf.core.http.request.ApiRequest;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AtfComponentTest
class MockMediaTypeContractTest {
    @ParameterizedTest
    @CsvSource({"application/json, true", "application/problem+json; charset=utf-8, true",
            "text/plain; profile=json, false", "application/notjson, false", "application/jsonp, false"})
    void jsonAndWrongContentTypeRoutesMakeOppositeDecisionsOnTheActualMime(String contentType, boolean json) {
        assertEquals(json, route(MockRequestBodyMode.JSON).matches(request(contentType, "{}")));
        assertEquals(!json, route(MockRequestBodyMode.WRONG_CONTENT_TYPE).matches(request(contentType, "{}")));
        assertFalse(route(MockRequestBodyMode.JSON).matches(request(contentType, "not JSON")));
        assertFalse(route(MockRequestBodyMode.WRONG_CONTENT_TYPE).matches(request(contentType, "not JSON")));
    }

    private static MockRoute route(MockRequestBodyMode mode) {
        return new MockRoute("POST", "/fixture", 201, "{}", Map.of(), Duration.ZERO,
                "*", Map.of(), Map.of(), mode.value(), null);
    }

    private static ApiRequest request(String contentType, String body) {
        return new ApiRequest(HttpRequest.BodyPublishers.ofString(body), "POST", "/fixture",
                Map.of(), Map.of("content-type", contentType), "none", null, body);
    }
}
