package atf.shop.tests.component.catalog;

import atf.reporting.contract.AtfAssertion;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.config.ShopScreen;
import atf.shop.config.ShopConfiguration;
import atf.shop.locators.ShopControl;
import atf.shop.pages.ShopBillingPage;
import atf.shop.pages.ShopCartPage;
import atf.shop.pages.ShopConfirmationPage;
import atf.shop.pages.ShopGuestCheckoutPage;
import atf.shop.pages.ShopHomePage;
import atf.shop.pages.account.ShopLoginPage;
import atf.shop.pages.account.ShopRegistrationPage;
import atf.shop.pages.account.ShopAccountPage;
import atf.shop.pages.ShopPaymentPage;
import atf.shop.pages.ShopProductPage;
import atf.shop.support.ShopPageContract;
import atf.shop.support.validation.ShopPageValidationResult;
import atf.shop.routes.ShopUiRoutes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.net.URI;

import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopPageContractTest {
    @Test
    void purchasePagesDeclareStableRoutesAndSemanticControls() {
        assertContract(ShopHomePage.class, ShopScreen.HOME, ShopUiRoutes.HOME, 16);
        assertContract(ShopProductPage.class, ShopScreen.PRODUCT,
                ShopUiRoutes.PRODUCT_PATTERN, 5);
        assertContract(ShopCartPage.class, ShopScreen.CART, ShopUiRoutes.CHECKOUT, 3);
        assertContract(ShopGuestCheckoutPage.class, ShopScreen.GUEST_CHECKOUT,
                ShopUiRoutes.CHECKOUT, 4);
        assertContract(ShopBillingPage.class, ShopScreen.BILLING,
                ShopUiRoutes.CHECKOUT, 7);
        assertContract(ShopPaymentPage.class, ShopScreen.PAYMENT,
                ShopUiRoutes.CHECKOUT, 2);
        assertContract(ShopConfirmationPage.class, ShopScreen.CONFIRMATION,
                ShopUiRoutes.CHECKOUT, 1);
        assertContract(ShopLoginPage.class, ShopScreen.LOGIN,
                ShopUiRoutes.AUTH_LOGIN, 3);
        assertContract(ShopAccountPage.class, ShopScreen.ACCOUNT,
                ShopUiRoutes.ACCOUNT, 1);
    }

    @Test
    void accountPagesAcceptResolvedRoutesInsteadOfGlobalConfiguration() throws NoSuchMethodException {
        assertEquals(void.class, ShopLoginPage.class.getMethod("open", URI.class).getReturnType());
        assertEquals(void.class, ShopRegistrationPage.class.getMethod("open", URI.class).getReturnType());
        assertThrows(NoSuchMethodException.class,
                () -> ShopLoginPage.class.getMethod("open", ShopConfiguration.class));
        assertThrows(NoSuchMethodException.class,
                () -> ShopRegistrationPage.class.getMethod("open", ShopConfiguration.class));
    }

    @Test
    void homeContractExposesDistinctControlsForStructureFiltersAndProductGrid() {
        var contract = ShopHomePage.class.getAnnotation(ShopPageContract.class);
        List<ShopControl> controls = List.of(contract.requiredControls());

        assertEquals(16, controls.stream().distinct().count());
        assertTrue(controls.containsAll(List.of(
                ShopControl.HOME_NAVIGATION,
                ShopControl.HOME_BRAND,
                ShopControl.HOME_BANNER,
                ShopControl.HOME_SORT,
                ShopControl.HOME_PRICE_RANGE,
                ShopControl.SEARCH_QUERY,
                ShopControl.HOME_PRODUCT_GRID,
                ShopControl.HOME_PRODUCT_NAMES,
                ShopControl.HOME_PRODUCT_IMAGES,
                ShopControl.HOME_PAGINATION,
                ShopControl.HOME_FOOTER)));
    }

    @Test
    void immutableValidationResultAggregatesAllFailures() {
        var result = new ShopPageValidationResult(ShopScreen.PRODUCT, List.of(
                new AtfAssertion("route", ShopUiRoutes.PRODUCT_PATTERN,
                        "/cart", false),
                new AtfAssertion("name", "Pliers", "Pliers", true)));

        assertFalse(result.passed());
        assertEquals("1/2 validations passed for PRODUCT", result.summary());
        assertThrows(UnsupportedOperationException.class, () -> result.assertions().clear());
    }

    private static void assertContract(Class<?> pageType, ShopScreen screen,
                                       String route, int controlCount) {
        ShopPageContract contract = pageType.getAnnotation(ShopPageContract.class);
        assertTrue(contract != null, pageType.getSimpleName() + " must be contract annotated");
        assertEquals(screen, contract.screen());
        assertEquals(route, contract.route());
        assertEquals(controlCount, contract.requiredControls().length);
        for (ShopControl control : contract.requiredControls()) {
            assertTrue(control.label().length() > 3);
        }
    }
}
