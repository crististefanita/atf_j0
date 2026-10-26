package atf.shop.tests.api.unit.auth;

import atf.api.testkit.AtfUnitTest;
import atf.api.core.http.response.ApiBodyEvidence;
import atf.shop.support.ShopLoginBodyRecovery;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ShopLoginBodyRecoveryTest {
    @Test
    void recoveredBodyIsReturnedAsTheEffectiveEvidence() {
        var result = ShopLoginBodyRecovery.attempt(
                () -> "{\"access_token\":\"<redacted>\"}");

        assertTrue(result.recovered());
        assertTrue(result.effectiveBody().contains("access_token"));
        assertFalse(ApiBodyEvidence.isUnavailable(result.effectiveBody()));
    }

    @Test
    void emptyRecoveryIsExplicitAndNeverLooksLikeValidJson() {
        var result = ShopLoginBodyRecovery.attempt(() -> " ");

        assertFalse(result.recovered());
        assertTrue(ApiBodyEvidence.isUnavailable(result.effectiveBody()));
        assertTrue(result.effectiveBody().contains("empty response body"));
    }

    @Test
    void transportFailurePreservesTheCauseForTheReport() {
        var result = ShopLoginBodyRecovery.attempt(
                () -> { throw new IOException("connection reset"); });

        assertFalse(result.recovered());
        assertTrue(result.effectiveBody().contains("IOException"));
        assertTrue(result.effectiveBody().contains("connection reset"));
    }

    @Test
    void nonSuccessHttpStatusRemainsVisibleInRecoveryEvidence() {
        var result = ShopLoginBodyRecovery.attempt(
                () -> { throw new IOException("HTTP status 503 for /users/login"); });

        assertFalse(result.recovered());
        assertTrue(result.effectiveBody().contains("HTTP status 503"));
        assertTrue(result.effectiveBody().contains("/users/login"));
    }
}
