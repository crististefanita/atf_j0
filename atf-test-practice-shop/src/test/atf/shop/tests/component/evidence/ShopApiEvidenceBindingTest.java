package atf.shop.tests.component.evidence;

import atf.testkit.AtfComponentTest;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.config.ShopValidationProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopApiEvidenceBindingTest {
    @Test
    void missingSelectionEnablesAllCheckpointsOnlyForUiApi() {
        var all = ShopApiEvidenceBinding.forProfile(ShopValidationProfile.UI_API, null);
        var none = ShopApiEvidenceBinding.forProfile(ShopValidationProfile.UI_ONLY, null);

        assertEquals("cart, login, order, payment, product, registration", all.displayValue());
        assertTrue(all.includes(ShopApiCheckpoint.REGISTRATION));
        assertTrue(all.includes(ShopApiCheckpoint.PRODUCT));
        assertTrue(all.includes(ShopApiCheckpoint.PAYMENT));
        assertTrue(all.includes(ShopApiCheckpoint.ORDER));
        assertFalse(none.includes(ShopApiCheckpoint.PRODUCT));
        assertEquals("", none.displayValue());
    }

    @Test
    void missingCsvInheritsProfileButExplicitlyBlankCsvSelectsNone() {
        var inherited = ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, null);
        var explicitlyEmpty = ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "  ");

        assertEquals(6, inherited.checkpoints().size());
        assertTrue(explicitlyEmpty.checkpoints().isEmpty());
    }

    @Test
    void explicitSelectionIsTypedAndDoesNotEnableUnselectedCheckpoints() {
        var binding = ShopApiEvidenceBinding.fromCsv(
                ShopValidationProfile.UI_API, "product, payment");

        assertTrue(binding.includes(ShopApiCheckpoint.PRODUCT));
        assertTrue(binding.includes(ShopApiCheckpoint.PAYMENT));
        assertFalse(binding.includes(ShopApiCheckpoint.LOGIN));
        assertEquals("payment, product", binding.displayValue());
    }

    @Test
    void unknownCheckpointFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> ShopApiEvidenceBinding.fromCsv(
                ShopValidationProfile.UI_API, "product,unknown"));
        assertThrows(IllegalArgumentException.class, () -> ShopApiCheckpoint.from(""));
    }
}
