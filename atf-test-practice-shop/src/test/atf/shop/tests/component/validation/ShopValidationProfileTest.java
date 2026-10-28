package atf.shop.tests.component.validation;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.config.ShopValidationProfile;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@ShopComponentTest
class ShopValidationProfileTest {
    @Test
    void uiApiIsTheDefaultTypedProfileAndUiOnlyIsAnExplicitFallback() {
        assertEquals(ShopValidationProfile.UI_API,
                ShopValidationProfile.from("ui-api"));
        assertEquals(ShopValidationProfile.UI_ONLY,
                ShopValidationProfile.from("ui-only"));
        assertEquals(ShopValidationProfile.UI_API,
                ShopValidationProfile.from(""));
        assertEquals(true, ShopValidationProfile.UI_API.apiEvidenceRequired());
        assertEquals(false, ShopValidationProfile.UI_ONLY.apiEvidenceRequired());
    }

    @Test
    void unsupportedProfilesFailBeforeTheBrowserStarts() {
        assertThrows(IllegalArgumentException.class,
                () -> ShopValidationProfile.from("ui-api-and-random-switches"));
    }

    @Test
    void ambiguousApiAliasFailsWithDistinctMigrationChoices() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ShopValidationProfile.from("API"));

        assertTrue(failure.getMessage().contains("ambiguous"));
        assertTrue(failure.getMessage().contains("'ui-api'"));
        assertTrue(failure.getMessage().contains("@AtfTestChannel(API)"));
    }
}
