package atf.autoex.tests.component.pages;

import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.AccountPage;
import atf.autoex.page.CheckoutPage;
import atf.autoex.page.HomePage;
import atf.autoex.page.ProductsPage;
import atf.autoex.support.account.AutoExAccountLifecycle;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.cart.AutoExCartJourneyPort;
import atf.autoex.support.journey.AutoExCatalogSearchPort;
import atf.autoex.support.journey.AutoExPurchaseJourney;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.autoex.ui.AutoExCheckoutPagePort;
import atf.autoex.ui.AutoExHomePagePort;
import atf.autoex.ui.AutoExUiActions;
import atf.autoex.tests.extension.AutoExComponentTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertNotNull;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AutoExComponentTest
class AutoExPagePortsComponentTest {
    @Test
    void homePageUsesItsNarrowPortForNavigationPromptAndCatalogEntry() {
        var calls = new ArrayList<String>();
        var configuration = AutoExConfigurationLoader.load();
        var home = port(AutoExHomePagePort.class, (method, arguments) -> switch (method) {
            case "configuration" -> configuration;
            case "navigate" -> { calls.add("navigate"); yield null; }
            case "dismissPrivacyPromptIfPresent" -> { calls.add("privacy"); yield false; }
            case "textFirst" -> { calls.add("read-heading"); yield "Automation Exercise"; }
            case "click" -> { calls.add("open-products"); yield null; }
            default -> throw new UnsupportedOperationException(method);
        });

        var page = new HomePage(home);
        assertEquals("Automation Exercise", page.open());
        page.openProducts();

        assertEquals(List.of("navigate", "privacy", "read-heading", "open-products"), calls);
    }

    @Test
    void checkoutPageUsesItsNarrowPortForPaymentAndKeepsTheUiActionOrder() {
        var calls = new ArrayList<String>();
        var payment = AutoExConfigurationLoader.load().payment();
        var checkout = port(AutoExCheckoutPagePort.class, (method, arguments) -> switch (method) {
            case "fill" -> { calls.add("fill"); yield null; }
            case "click" -> { calls.add("click"); yield null; }
            case "dismissGoogleVignetteIfPresent" -> { calls.add("dismiss-vignette"); yield false; }
            case "text" -> {
                calls.add("read");
                yield AutoExSelectors.PAYMENT_HEADING.equals(arguments[0])
                        ? "Payment" : "Order placed";
            }
            default -> throw new UnsupportedOperationException(method);
        });

        var page = new CheckoutPage(checkout);
        assertEquals("Payment", page.placeOrder("Synthetic test order"));
        assertEquals("Order placed", page.payWithDemoCard(payment));

        assertEquals(List.of("fill", "click", "dismiss-vignette", "read",
                "fill", "fill", "fill", "fill", "fill", "click", "read"), calls);
    }

    @Test
    void productSearchNeedsOnlyUiActionsAndExplicitExpectedProduct() {
        var calls = new ArrayList<String>();
        var ui = port(AutoExUiActions.class, (method, arguments) -> switch (method) {
            case "fill" -> { calls.add("fill-search"); yield null; }
            case "click" -> { calls.add("submit-search"); yield null; }
            case "waitForText" -> { calls.add("wait-for-configured-result:" + arguments[1]); yield null; }
            case "clickFirst" -> { calls.add("open-first-result"); yield null; }
            case "text" -> {
                calls.add("read-result");
                yield AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME.equals(arguments[0])
                        ? "Blue Top" : "Product details";
            }
            default -> throw new UnsupportedOperationException(method);
        });

        var products = new ProductsPage(ui, "Blue Top");
        products.search("Blue");
        assertEquals("Blue Top", products.firstProductName());
        assertEquals("Product details", products.openFirstProduct().details());

        assertEquals(List.of("fill-search", "submit-search", "read-result",
                "wait-for-configured-result:Blue Top", "open-first-result", "read-result"), calls);
    }

    @Test
    void uiCatalogSearchUsesNarrowJourneyPortWithoutApiInspectionCapabilities() {
        var calls = new ArrayList<String>();
        var configuration = AutoExConfigurationLoader.load();
        String searchTerm = configuration.catalog().searchTerm();
        String expectedUrl = "%s?%s=%s".formatted(AutoExRoutes.PRODUCTS.resolve(configuration.baseUrl()),
                AutoExRoutes.SEARCH_PARAMETER, URLEncoder.encode(searchTerm, StandardCharsets.UTF_8));
        var search = port(AutoExCatalogSearchPort.class, (method, arguments) -> switch (method) {
            case "configuration" -> configuration;
            case "navigate" -> { calls.add("navigate"); yield null; }
            case "dismissPrivacyPromptIfPresent" -> { calls.add("privacy-prompt"); yield false; }
            case "textFirst" -> { calls.add("read-store-heading"); yield "Automation Exercise"; }
            case "click" -> { calls.add("click"); yield null; }
            case "fill" -> { calls.add("fill"); yield null; }
            case "text" -> { calls.add("read-search-result"); yield configuration.catalog().productName(); }
            case "currentUrl" -> { calls.add("read-search-url"); yield expectedUrl; }
            case "checkpoint" -> {
                calls.add("checkpoint:" + ((AutoExTraceStep) arguments[0]).title());
                yield null;
            }
            default -> throw new UnsupportedOperationException(method);
        });

        var products = AutoExPurchaseJourney.searchConfiguredProduct(search);

        assertEquals(configuration.catalog().productName(), products.firstProductName());
        assertEquals(List.of("navigate", "privacy-prompt", "read-store-heading",
                "checkpoint:" + AutoExPurchaseTraceStep.STORE_OPEN.title(), "click", "fill", "click",
                "read-search-result", "read-search-url",
                "checkpoint:" + AutoExPurchaseTraceStep.PRODUCT_SEARCH.title(), "read-search-result"), calls);
        assertFalse(List.of(AutoExCartJourneyPort.class.getMethods()).stream()
                .map(java.lang.reflect.Method::getName)
                .anyMatch(name -> List.of("inspectCatalogApi", "apiCheckpoint", "observesApiTraffic",
                        "markCausalApiTrafficBaseline", "cartAddCheckpoint").contains(name)));
    }

    @Test
    void homeNavigationFailureStopsBeforePromptHandlingOrScreenRead() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("navigation failed");
        var home = port(AutoExHomePagePort.class, (method, arguments) -> switch (method) {
            case "configuration" -> AutoExConfigurationLoader.load();
            case "navigate" -> { calls.add("navigate"); throw failure; }
            case "dismissPrivacyPromptIfPresent" -> { calls.add("privacy"); yield false; }
            case "textFirst" -> { calls.add("read-heading"); yield "unreachable"; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class, () -> new HomePage(home).open()));
        assertEquals(List.of("navigate"), calls);
    }

    @Test
    void missingStoreHeadingStopsCatalogJourneyBeforeCheckpointOrSearch() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("store heading was not found");
        var configuration = AutoExConfigurationLoader.load();
        var search = port(AutoExCatalogSearchPort.class, (method, arguments) -> switch (method) {
            case "configuration" -> configuration;
            case "navigate" -> { calls.add("navigate"); yield null; }
            case "dismissPrivacyPromptIfPresent" -> { calls.add("privacy-prompt"); yield false; }
            case "textFirst" -> { calls.add("read-store-heading"); throw failure; }
            case "checkpoint" -> { calls.add("checkpoint"); yield null; }
            case "click", "fill" -> { calls.add(method); yield null; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> AutoExPurchaseJourney.searchConfiguredProduct(search)));
        assertEquals(List.of("navigate", "privacy-prompt", "read-store-heading"), calls);
    }

    @Test
    void checkoutActionFailureDoesNotDismissPromptOrReadPaymentState() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("submit failed");
        var checkout = port(AutoExCheckoutPagePort.class, (method, arguments) -> switch (method) {
            case "fill" -> { calls.add("fill"); yield null; }
            case "click" -> { calls.add("click"); throw failure; }
            case "dismissGoogleVignetteIfPresent" -> { calls.add("dismiss-vignette"); yield false; }
            case "text" -> { calls.add("read"); yield "unreachable"; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new CheckoutPage(checkout).placeOrder("Synthetic test order")));
        assertEquals(List.of("fill", "click"), calls);
    }

    @Test
    void missingPaymentPageHeadingStopsBeforeCardEntry() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("payment page heading was not found");
        var checkout = port(AutoExCheckoutPagePort.class, (method, arguments) -> switch (method) {
            case "fill" -> { calls.add("fill-order-comment"); yield null; }
            case "click" -> { calls.add("submit-order-review"); yield null; }
            case "dismissGoogleVignetteIfPresent" -> { calls.add("dismiss-vignette"); yield false; }
            case "text" -> { calls.add("read-payment-heading"); throw failure; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new CheckoutPage(checkout).placeOrder("Synthetic test order")));
        assertEquals(List.of("fill-order-comment", "submit-order-review", "dismiss-vignette",
                "read-payment-heading"), calls);
    }

    @Test
    void paymentActionFailureDoesNotReadAnOrderConfirmation() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("payment failed");
        var checkout = port(AutoExCheckoutPagePort.class, (method, arguments) -> switch (method) {
            case "fill" -> { calls.add("fill"); yield null; }
            case "click" -> { calls.add("click"); throw failure; }
            case "text" -> { calls.add("read"); yield "unreachable"; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new CheckoutPage(checkout).payWithDemoCard(
                        AutoExConfigurationLoader.load().payment())));
        assertEquals(List.of("fill", "fill", "fill", "fill", "fill", "click"), calls);
    }

    @Test
    void missingSearchResultTimesOutBeforeOpeningProductWithoutRetry() {
        var calls = new ArrayList<String>();
        var timeout = new IllegalStateException("timed out waiting for configured search result");
        var ui = port(AutoExUiActions.class, (method, arguments) -> switch (method) {
            case "waitForText" -> { calls.add("wait-for-configured-result"); throw timeout; }
            case "clickFirst" -> { calls.add("open-first-result"); yield null; }
            default -> throw new UnsupportedOperationException(method);
        });

        var products = new ProductsPage(ui, "Blue Top");
        assertSame(timeout, assertThrows(IllegalStateException.class, products::openFirstProduct));
        assertEquals(List.of("wait-for-configured-result"), calls);
    }

    @Test
    void missingPaymentConfirmationStopsCheckoutBeforeTheCallerCanContinue() {
        var calls = new ArrayList<String>();
        var failure = new IllegalStateException("payment confirmation was not found");
        var checkout = port(AutoExCheckoutPagePort.class, (method, arguments) -> switch (method) {
            case "fill" -> { calls.add("fill"); yield null; }
            case "click" -> { calls.add("pay-and-confirm"); yield null; }
            case "text" -> { calls.add("read-confirmation"); throw failure; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new CheckoutPage(checkout).payWithDemoCard(
                        AutoExConfigurationLoader.load().payment())));
        assertEquals(List.of("fill", "fill", "fill", "fill", "fill",
                "pay-and-confirm", "read-confirmation"), calls);
    }

    @Test
    void missingSignedInIdentityStopsAccountFlowAfterOneBoundedWait() {
        var calls = new ArrayList<String>();
        var configuration = AutoExConfigurationLoader.load();
        var account = AutoExTestAccount.create(configuration.registration());
        var timeout = new IllegalStateException("timed out waiting for signed-in identity");
        var page = port(AutoExAccountPagePort.class, (method, arguments) -> switch (method) {
            case "accountLifecycle" -> new AutoExAccountLifecycle();
            case "configuration" -> configuration;
            case "visible" -> { calls.add("check-login-form"); yield false; }
            case "click" -> {
                calls.add(AutoExSelectors.HEADER_LOGIN.equals(arguments[0]) ? "open-login" : "submit-login");
                yield null;
            }
            case "fill" -> {
                calls.add(AutoExSelectors.LOGIN_EMAIL.equals(arguments[0]) ? "fill-email" : "fill-password");
                yield null;
            }
            case "waitForText" -> { calls.add("wait-for-signed-in-identity"); throw timeout; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(timeout, assertThrows(IllegalStateException.class,
                () -> new AccountPage(page).login(account)));
        assertEquals(List.of("check-login-form", "open-login", "fill-email", "fill-password",
                "submit-login", "wait-for-signed-in-identity"), calls);
    }

    @Test
    void missingAccountCreationResultKeepsCleanupArmedAndStopsBeforeCheckpointOrContinue() {
        var calls = new ArrayList<String>();
        var configuration = AutoExConfigurationLoader.load();
        var account = AutoExTestAccount.create(configuration.registration());
        var lifecycle = new AutoExAccountLifecycle();
        var cleanup = new AtomicReference<AutoExAccountPagePort.AccountCleanup>();
        var failure = new IllegalStateException("account creation result heading was not found");
        var page = port(AutoExAccountPagePort.class, (method, arguments) -> switch (method) {
            case "configuration" -> configuration;
            case "accountLifecycle" -> lifecycle;
            case "onFailureCleanup" -> {
                calls.add("arm-cleanup");
                cleanup.set((AutoExAccountPagePort.AccountCleanup) arguments[0]);
                yield null;
            }
            case "clickLast" -> { calls.add("click-last:" + arguments[0]); yield null; }
            case "click" -> { calls.add("click:" + arguments[0]); yield null; }
            case "fill" -> { calls.add("fill:" + arguments[0]); yield null; }
            case "select" -> { calls.add("select:" + arguments[0]); yield null; }
            case "text" -> { calls.add("text:" + arguments[0]); throw failure; }
            case "checkpoint" -> { calls.add("checkpoint"); yield null; }
            default -> throw new UnsupportedOperationException(method);
        });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new AccountPage(page).registerFromStore(account)));
        assertEquals(List.of("click:" + AutoExSelectors.HEADER_LOGIN,
                "fill:" + AutoExSelectors.SIGNUP_NAME, "fill:" + AutoExSelectors.SIGNUP_EMAIL,
                "click:" + AutoExSelectors.SIGNUP_BUTTON,
                "click:" + AutoExSelectors.ACCOUNT_GENDER_MR,
                "fill:" + AutoExSelectors.ACCOUNT_PASSWORD,
                "select:" + AutoExSelectors.ACCOUNT_DAYS, "select:" + AutoExSelectors.ACCOUNT_MONTHS,
                "select:" + AutoExSelectors.ACCOUNT_YEARS,
                "fill:" + AutoExSelectors.ACCOUNT_FIRST_NAME, "fill:" + AutoExSelectors.ACCOUNT_LAST_NAME,
                "fill:" + AutoExSelectors.ACCOUNT_COMPANY, "fill:" + AutoExSelectors.ACCOUNT_ADDRESS_1,
                "fill:" + AutoExSelectors.ACCOUNT_ADDRESS_2, "select:" + AutoExSelectors.ACCOUNT_COUNTRY,
                "fill:" + AutoExSelectors.ACCOUNT_STATE, "fill:" + AutoExSelectors.ACCOUNT_CITY,
                "fill:" + AutoExSelectors.ACCOUNT_POSTAL_CODE, "fill:" + AutoExSelectors.ACCOUNT_MOBILE,
                "arm-cleanup",
                "click:" + AutoExSelectors.CREATE_ACCOUNT,
                "text:" + AutoExSelectors.ACCOUNT_RESULT_HEADING), calls);
        assertTrue(lifecycle.creationWasAttempted());
        assertNotNull(cleanup.get());
    }

    private static <T> T port(Class<T> type, PageCall call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> call.invoke(method.getName(), arguments)));
    }

    @FunctionalInterface
    private interface PageCall {
        Object invoke(String method, Object[] arguments);
    }
}
