package atf.autoex.tests.component;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.support.validation.AutoExStoreAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;

/** Independent observed text protects the shared verdicts used by pages and journeys. */
@AutoExComponentTest
class AutoExStoreAssertionsComponentTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("observations")
    void observedBusinessStateDeterminesTheVerdict(String name, Function<String, AtfAssertion> contract,
                                                   String observed, boolean expectedPass) {
        AtfAssertion result = contract.apply(observed);
        assertEquals(expectedPass, result.passed(), name);
        assertEquals(observed, result.actual());
    }

    static Stream<Arguments> observations() {
        return Stream.of(
                sample("store ready", AutoExStoreAssertions::storeAvailability,
                        "Automation Exercise — Full-Fledged practice website for Automation Engineers", true),
                sample("store error", AutoExStoreAssertions::storeAvailability, "Service unavailable", false),
                sample("created", AutoExStoreAssertions::accountCreated, "ACCOUNT CREATED!", true),
                sample("creation rejected", AutoExStoreAssertions::accountCreated, "ACCOUNT NOT CREATED!", false),
                sample("deleted", AutoExStoreAssertions::accountDeleted, "ACCOUNT DELETED!", true),
                sample("deletion rejected", AutoExStoreAssertions::accountDeleted, "ACCOUNT NOT DELETED!", false),
                sample("added with detail", AutoExStoreAssertions::cartAddition,
                        "Added! Your product has been added to cart.", true),
                sample("title alone", AutoExStoreAssertions::cartAddition, "Added!", false),
                sample("detail alone", AutoExStoreAssertions::cartAddition, "Your product has been added to cart.", false),
                sample("empty", AutoExStoreAssertions::emptyCart, "Cart is empty! Click here to buy products.", true),
                sample("cart populated", AutoExStoreAssertions::emptyCart, "2 products in cart", false),
                sample("in stock", AutoExStoreAssertions::stockAvailability, "Availability: In Stock", true),
                sample("out of stock", AutoExStoreAssertions::stockAvailability, "Availability: Out of Stock", false),
                sample("payment page", AutoExStoreAssertions::paymentPage, "Payment", true),
                sample("payment error page", AutoExStoreAssertions::paymentPage, "Payment unavailable", false),
                sample("order placed", AutoExStoreAssertions::orderPlaced, "ORDER PLACED!", true),
                sample("different order heading", AutoExStoreAssertions::orderPlaced, "Order Placed! Error", false),
                sample("order confirmed", AutoExStoreAssertions::orderConfirmation,
                        "Congratulations! Your order has been confirmed!", true),
                sample("order not confirmed", AutoExStoreAssertions::orderConfirmation,
                        "Your order has not been confirmed!", false));
    }

    @ParameterizedTest
    @MethodSource("contracts")
    void missingOrBlankUiEvidenceNeverPasses(Function<String, AtfAssertion> contract) {
        assertFalse(contract.apply(null).passed());
        assertFalse(contract.apply("").passed());
        assertFalse(contract.apply("   ").passed());
        assertEquals("<missing>", contract.apply(null).actual());
    }

    static Stream<Function<String, AtfAssertion>> contracts() {
        return Stream.of(AutoExStoreAssertions::storeAvailability, AutoExStoreAssertions::accountCreated,
                AutoExStoreAssertions::accountDeleted, AutoExStoreAssertions::cartAddition,
                AutoExStoreAssertions::emptyCart, AutoExStoreAssertions::stockAvailability,
                AutoExStoreAssertions::paymentPage, AutoExStoreAssertions::orderPlaced,
                AutoExStoreAssertions::orderConfirmation);
    }

    private static Arguments sample(String name, Function<String, AtfAssertion> contract,
                                    String observed, boolean passed) {
        return Arguments.of(name, contract, observed, passed);
    }
}
