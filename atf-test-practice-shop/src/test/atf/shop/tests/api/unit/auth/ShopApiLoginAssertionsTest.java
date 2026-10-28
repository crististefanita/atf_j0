package atf.shop.tests.api.unit.auth;

import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.testkit.AtfUnitTest;
import atf.shop.support.api.ShopApiAssertions;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component contract for a JSON login response whose browser body is deferred. */
@AtfUnitTest
class ShopApiLoginAssertionsTest {
    @Test
    void responseObservationDistinguishesEmptyUnavailableAndPresentJson() {
        var bodies = Map.of(
                "", "Empty response body",
                "<body unavailable: Browser observation — payload released>",
                "<body unavailable: Browser observation — payload released>",
                "{\"message\":\"Unauthorized\"}", "{ \"message\" : \"Unauthorized\" }");
        bodies.forEach((body, observed) -> {
            var exchange = new ShopApiNetworkJournal.Exchange(HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                    ShopApiScenarioFixture.API_ENDPOINT + ShopApiRoutes.LOGIN, 401,
                    "{\"email\":\"<redacted>\",\"password\":\"<redacted>\"}", body,
                    HttpHeaderProfiles.JSON_CONTENT, HttpHeaderProfiles.JSON_CONTENT);
            var row = ShopApiAssertions.login(exchange).get(2);
            assertEquals(observed, row.actual());
            assertTrue(!row.passed(), "Rejected login must retain its original verdict");
        });
    }

    @Test
    void acceptsJsonTransportWhenPlaywrightCannotExposeCompressedBody() {
        var login = new ShopApiNetworkJournal.Exchange(
                HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                ShopApiScenarioFixture.API_ENDPOINT + ShopApiRoutes.LOGIN,
                ShopApiHttpStatus.OK.code(),
                "{\"email\":\"<redacted>\",\"password\":\"<redacted>\"}", "",
                HttpHeaderProfiles.JSON_CONTENT, Map.of(
                HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON,
                HttpHeader.CONTENT_LENGTH.value(), "384"));

        var assertions = ShopApiAssertions.login(login);

        assertTrue(assertions.stream().allMatch(assertion -> assertion.passed()),
                () -> assertions.toString());
        assertEquals("Body not captured; content-length=384", assertions.get(2).actual());
    }

    @Test
    void responseBodyRecoveryPreservesTraceAndRedactsToken() {
        var captured = new ShopApiNetworkJournal.Exchange(
                HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                ShopApiScenarioFixture.API_ENDPOINT + ShopApiRoutes.LOGIN,
                ShopApiHttpStatus.OK.code(),
                "{\"email\":\"<redacted>\",\"password\":\"***REDACTED***\"}",
                "<body unavailable: Chromium released response payload>",
                Map.of(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON), Map.of(
                HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON,
                HttpHeader.CONTENT_LENGTH.value(), "384"))
                .withTrace(7, java.time.Instant.parse("2026-10-20T10:00:00Z"), "authentication");

        var recovered = captured.withResponseBody("{\"token\":\"raw-token\",\"user\":\"ATF Practice\"}");

        assertTrue(recovered.responseBody().contains("\"token\" : \"<redacted>\""));
        assertTrue(recovered.responseBody().contains("\"user\" : \"ATF Practice\""));
        assertEquals(captured.sequence(), recovered.sequence());
        assertEquals(captured.signature(), recovered.signature());
        assertTrue(recovered.digest() != null && !recovered.digest().isBlank());
    }
}
