package atf.autoex.tests.component.purchase;

import atf.autoex.business.checkout.AutoExCheckoutValidationException;
import atf.autoex.config.AutoExConfiguration.*;
import atf.autoex.flow.purchase.*;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.ProductsPage;
import atf.autoex.support.account.AutoExAccountLifecycle;
import atf.autoex.support.journey.*;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.reporting.contract.AtfAssertion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Public stage/page wiring with observed registration identity; no reflection into flow state. */
@AutoExComponentTest
class AutoExCheckoutUiGateComponentTest {
    private static final Catalog PRODUCT = new Catalog("Blue Top", "1", "Blue Top", "Rs. 500", 2);
    private static final AutoExPurchaseInputs INPUTS = new AutoExPurchaseInputs(PRODUCT,
            new Registration("10", "May", "1990", "ATF", "Customer", "Demo", "Street", "Area", "United States", "State", "City", "10001", "5550100"),
            new Payment("ATF Demo", "4111111111111111", "123", "12", "2030"), URI.create("https://fixture.test/"));

    @ParameterizedTest @ValueSource(strings = {"foreign", "absent", "wrong-cart", "owned"})
    void theRealReviewStageChecksTheRegisteredOwnerAndCurrentCartBeforeItsOnlyCheckout(String mode) {
        var probe = new Probe(mode); var session = probe.session();
        var flow = new AutoExPurchaseUiFlow(session, new ProductsPage(session, PRODUCT.productName()), INPUTS);
        flow.registerOwnedCustomer(); probe.calls.clear();
        if (mode.equals("owned")) flow.reviewCheckout();
        else assertThrows(AutoExCheckoutValidationException.class, flow::reviewCheckout,
                "An unsafe owner or cart stops checkout at the business gate");
        assertEquals(5, probe.gateRows.size(), "The checkout gate retains all five identity, cart and total comparisons");
        assertTrue(probe.calls.indexOf("observe-customer") >= 0
                        && probe.calls.indexOf("observe-customer") < probe.calls.indexOf("publish-gate"),
                "The page's bounded customer observation precedes the business gate publication");
        assertEquals(probe.registeredName, probe.gateRows.getFirst().expected(), "The gate's expected owner comes from this registration");
        assertEquals(mode.equals("owned") ? 1 : 0, Collections.frequency(probe.calls, "checkout"),
                "Only the accepted owner and current cart permit one checkout activation; unsafe modes dispatch none");
        assertTrue(probe.calls.indexOf("publish-gate") >= 0, "Every checkout gate publishes its original comparisons");
        if (mode.equals("owned")) assertTrue(probe.calls.indexOf("publish-gate") < probe.calls.indexOf("checkout"),
                "The accepted business gate is published before the only checkout activation");
        else assertFalse(probe.gateRows.stream().allMatch(AtfAssertion::passed),
                "At least one independent comparison refuses the unsafe owner or cart");
    }

    private static final class Probe implements InvocationHandler {
        final String mode;
        final AutoExAccountLifecycle lifecycle = new AutoExAccountLifecycle();
        final List<String> calls = new ArrayList<>();
        String registeredName;
        List<AtfAssertion> gateRows = List.of();
        Probe(String mode) { this.mode = mode; }
        AutoExPurchaseSession session() { return (AutoExPurchaseSession) Proxy.newProxyInstance(
                AutoExPurchaseSession.class.getClassLoader(), new Class<?>[]{AutoExPurchaseSession.class}, this); }
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "accountLifecycle" -> lifecycle;
                case "onFailureCleanup", "clearFailureCleanup", "select", "waitForVisible", "waitForText" -> null;
                case "fill" -> { if (args[0].equals(AutoExSelectors.SIGNUP_NAME)) registeredName = (String) args[1]; yield null; }
                case "click", "clickLast", "clickFirst" -> { if (args[0].equals(AutoExSelectors.CHECKOUT)) calls.add("checkout"); yield null; }
                case "visible" -> !mode.equals("absent");
                case "observeCustomerName" -> {
                    calls.add("observe-customer");
                    yield mode.equals("absent") ? "" : text(AutoExSelectors.LOGGED_IN_NAME);
                }
                case "text" -> text((String) args[0]);
                case "checkpoint" -> {
                    if (args[0] == AutoExPurchaseTraceStep.CHECKOUT_PRECONDITIONS) {
                        calls.add("publish-gate"); gateRows = ((List<?>) args[1]).stream().map(AtfAssertion.class::cast).toList();
                    }
                    yield null;
                }
                default -> throw new AssertionError("Unexpected page capability: " + method.getName());
            };
        }
        private String text(String selector) {
            if (selector.equals(AutoExSelectors.ACCOUNT_RESULT_HEADING)) return "ACCOUNT CREATED!";
            if (selector.equals(AutoExSelectors.LOGGED_IN_NAME)) return mode.equals("foreign") ? "other customer" : registeredName;
            if (selector.equals(AutoExSelectors.DELIVERY_ADDRESS) || selector.equals(AutoExSelectors.BILLING_ADDRESS))
                return "Mr. ATF Customer\nDemo\nStreet\nArea\nCity State 10001\nUnited States\n5550100";
            if (selector.endsWith(AutoExSelectors.CART_PRODUCT_NAME)) return mode.equals("wrong-cart") ? "Other" : "Blue Top";
            if (selector.endsWith(AutoExSelectors.CART_PRICE)) return "Rs. 500";
            if (selector.endsWith(AutoExSelectors.CART_QUANTITY)) return "2";
            if (selector.endsWith(AutoExSelectors.CART_TOTAL)) return "Rs. 1000";
            if (selector.equals(AutoExSelectors.ORDER_TOTAL)) return "Rs. 1000";
            throw new AssertionError("Unexpected field observation: " + selector);
        }
    }
}
