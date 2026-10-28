package atf.autoex.tests.component.journey;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.AccountPage;
import atf.autoex.support.journey.AutoExPurchaseSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;

/** Verifies checkout addresses are semantically ready before their text is read. */
@AutoExComponentTest
class AutoExCheckoutAddressComponentTest {
    private static final String DISPLAY_NAME = "ATF Demo Customer";

    @Test
    void bothAddressesWaitForConfiguredNameBeforeReadingTheRenderedAddress() {
        var calls = new ArrayList<String>();
        AutoExPurchaseSession session = sessionRecording(calls);
        var account = new AccountPage(session);

        String delivery = account.deliveryAddress(DISPLAY_NAME);
        String billing = account.billingAddress(DISPLAY_NAME);

        assertEquals("ATF Demo Customer\nDelivery details", delivery);
        assertEquals("ATF Demo Customer\nBilling details", billing);
        assertEquals(List.of(
                "wait:" + AutoExSelectors.DELIVERY_ADDRESS + ":" + DISPLAY_NAME,
                "read:" + AutoExSelectors.DELIVERY_ADDRESS,
                "wait:" + AutoExSelectors.BILLING_ADDRESS + ":" + DISPLAY_NAME,
                "read:" + AutoExSelectors.BILLING_ADDRESS), calls);
    }

    private static AutoExPurchaseSession sessionRecording(List<String> calls) {
        return (AutoExPurchaseSession) Proxy.newProxyInstance(
                AutoExPurchaseSession.class.getClassLoader(),
                new Class<?>[]{AutoExPurchaseSession.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("waitForText")) {
                        calls.add("wait:" + arguments[0] + ":" + arguments[1]);
                        return null;
                    }
                    if (method.getName().equals("text")) {
                        String selector = (String) arguments[0];
                        calls.add("read:" + selector);
                        return selector.equals(AutoExSelectors.DELIVERY_ADDRESS)
                                ? DISPLAY_NAME + "\nDelivery details"
                                : DISPLAY_NAME + "\nBilling details";
                    }
                    throw new UnsupportedOperationException("Unexpected session call: " + method.getName());
                });
    }
}
