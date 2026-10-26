package atf.shop.tests.api.component.contract;

import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.response.ApiResponse;
import atf.core.config.ApiTransportMode;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.ApiTransportProvenance;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.tests.api.support.ShopApiCallComponentTest;
import atf.shop.tests.api.support.ShopApiCallAuditContextTestSupport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ShopApiCallComponentTest
class ShopApiClientContractTest {
    @Test
    void localTransportIsPublishedAsAcceptanceProvenance() {
        var client = ShopApiClient.forTransport(ShopApiScenarioFixture.API_ENDPOINT,
                ShopApiComponentFixture.recordedTransport(
                        jsonTransport(ShopApiScenarioFixture.LOGIN_TOKEN_BODY),
                        ApiTransportProvenance.ACCEPTANCE_MOCK));

        ShopApiCallAuditContextTestSupport.expectNext("local transport login", ShopApiHttpStatus.OK);
        var result = client.login(ShopApiScenarioFixture.validCredentials());

        assertEquals(ApiTransportProvenance.ACCEPTANCE_MOCK,
                result.loginResult().provenance());
    }

    @Test
    void explicitlyLiveTransportIsPublishedAsLiveProvenance() {
        var client = ShopApiClient.forTransport(ShopApiScenarioFixture.API_ENDPOINT,
                ShopApiComponentFixture.recordedTransport(
                        jsonTransport(ShopApiScenarioFixture.LOGIN_TOKEN_BODY),
                        ApiTransportProvenance.ACCEPTANCE_MOCK), ApiTransportMode.LIVE);

        ShopApiCallAuditContextTestSupport.expectNext("explicit live transport login",
                ShopApiHttpStatus.OK);
        var result = client.login(ShopApiScenarioFixture.validCredentials());

        assertEquals(ApiTransportProvenance.LIVE_HTTP,
                result.loginResult().provenance());
    }

    @Test
    void malformedSuccessfulLoginResponseFailsWithCauseInsteadOfEmptyToken() {
        var client = ShopApiClient.forTransport(ShopApiScenarioFixture.API_ENDPOINT,
                ShopApiComponentFixture.recordedTransport(jsonTransport("not-json"),
                        ApiTransportProvenance.ACCEPTANCE_MOCK));

        ShopApiCallAuditContextTestSupport.expectNext("malformed login still returns HTTP 200",
                ShopApiHttpStatus.OK);
        assertThrows(IllegalStateException.class, () -> client.login(
                ShopApiScenarioFixture.validCredentials()));
    }

    @Test
    void successfulLoginProducesReusableAuthenticatedSession() {
        var client = ShopApiComponentFixture.client();

        ShopApiCallAuditContextTestSupport.expectNext("valid login", ShopApiHttpStatus.OK);
        var session = client.login(ShopApiScenarioFixture.validCredentials());

        assertTrue(session.authenticated());
        assertEquals(ShopApiHttpStatus.OK.code(), session.loginResult().response().status());
    }

    @Test
    void invalidLoginClearsPreviousBearer() {
        var client = ShopApiComponentFixture.client();

        ShopApiCallAuditContextTestSupport.expectNext("valid login before invalid login",
                ShopApiHttpStatus.OK);
        client.login(ShopApiScenarioFixture.validCredentials());
        ShopApiCallAuditContextTestSupport.expectNext("invalid login is unauthorized",
                ShopApiHttpStatus.UNAUTHORIZED);
        var invalidSession = client.login(ShopApiScenarioFixture.invalidCredentials());

        assertFalse(invalidSession.authenticated());
        assertEquals(ShopApiHttpStatus.UNAUTHORIZED.code(),
                invalidSession.loginResult().response().status());
        assertEquals("", client.bearerToken());
    }

    @Test
    void closingClientClearsSessionAndRejectsLaterNetworkUse() {
        var client = ShopApiComponentFixture.client();
        ShopApiCallAuditContextTestSupport.expectNext("valid login before session close",
                ShopApiHttpStatus.OK);
        client.login(ShopApiScenarioFixture.validCredentials());

        client.close();

        assertEquals("", client.bearerToken());
        assertThrows(IllegalStateException.class,
                () -> client.login(ShopApiScenarioFixture.validCredentials()));
    }

    private static ApiTransport jsonTransport(String responseBody) {
        return (request, url, auth) -> CompletableFuture.completedFuture(new ApiResponse(
                ShopApiHttpStatus.OK.code(), responseBody,
                HttpHeaderProfiles.JSON_CONTENT,
                Duration.ZERO, url));
    }
}
