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
