package atf.autoex.tests.component.pages;

import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.CheckoutPage;
import atf.autoex.page.HomePage;
import atf.autoex.page.ProductsPage;
import atf.autoex.ui.AutoExCheckoutPagePort;
import atf.autoex.ui.AutoExHomePagePort;
import atf.autoex.ui.AutoExUiActions;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest
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

    private static <T> T port(Class<T> type, PageCall call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> call.invoke(method.getName(), arguments)));
    }

    @FunctionalInterface
    private interface PageCall {
        Object invoke(String method, Object[] arguments);
    }
}
