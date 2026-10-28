package atf.autoex.tests.component.journey;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightUiSession;
import atf.autoex.page.ProductPage;
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.config.AutoExConfiguration.Registration;
import atf.autoex.support.checkout.AutoExCheckoutAssertions;
import atf.autoex.support.checkout.AutoExInvoiceAssertions;
import atf.autoex.support.checkout.AutoExOrderLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves propagation of customer and order data; no browser or report layout is simulated. */
@AutoExComponentTest
class AutoExPurchaseBusinessComponentTest {
    private static final Registration CUSTOMER = new Registration("10", "May", "1990",
            "ATF", "Customer", "Test Ltd", "10 Test Avenue", "Test District",
            "United States", "Test State", "Test City", "10001", "5550100100");
    private static final Catalog PRODUCT = new Catalog("Blue Top", "1", "Blue Top", "Rs. 500", 2);
    private static final String TOTAL = "Rs. 1000";
    private static final String ADDRESS = """
            Your delivery address
            Mr. ATF Customer
            Test Ltd
            10 Test Avenue
            Test District
            Test City Test State 10001
            United States
            5550100100
            """;

    @Test
    void fullAddressPreservesEveryConfiguredFieldDespitePresentationWhitespace() {
        String rendered = ADDRESS.replace("Test City Test State", "Test City   Test State");
        assertTrue(AutoExCheckoutAssertions.address("Delivery", CUSTOMER, rendered)
                .stream().allMatch(AtfAssertion::passed));
        assertTrue(AutoExCheckoutAssertions.address("Billing", CUSTOMER, rendered)
                .stream().allMatch(AtfAssertion::passed));
    }

    @ParameterizedTest
    @MethodSource("addressChanges")
    void unchangedRecipientCannotHideAnIncorrectAddressField(String original, String replacement) {
        String incorrect = ADDRESS.replace(original, replacement);
        assertTrue(AutoExCheckoutAssertions.address("Delivery", CUSTOMER, incorrect)
                .stream().anyMatch(assertion -> !assertion.passed()));
    }

    static Stream<Arguments> addressChanges() {
        return Stream.of(
                Arguments.of("ATF Customer", "ATF Customer2"),
                Arguments.of("Test Ltd", "Other Ltd"),
                Arguments.of("10 Test Avenue", "110 Test Avenue"),
                Arguments.of("Test District", "Other District"),
                Arguments.of("10001", "100010"),
                Arguments.of("United States", "United Kingdom"),
                Arguments.of("5550100100", "55501001001"));
    }

    @Test
    void checkoutMustPreserveTheCartProductPriceQuantityAndTotal() {
        assertTrue(AutoExCheckoutAssertions.orderLine(PRODUCT, TOTAL,
                new AutoExOrderLine("Blue Top", "Rs. 500", "2", TOTAL))
                .stream().allMatch(AtfAssertion::passed));
    }

    @ParameterizedTest
    @MethodSource("orderChanges")
    void aCorrectProductNameCannotHideOtherOrderDrift(AutoExOrderLine incorrect) {
        assertTrue(AutoExCheckoutAssertions.orderLine(PRODUCT, TOTAL, incorrect)
                .stream().anyMatch(assertion -> !assertion.passed()));
    }

    static Stream<AutoExOrderLine> orderChanges() {
        return Stream.of(
                new AutoExOrderLine("Blue Top 2", "Rs. 500", "2", TOTAL),
                new AutoExOrderLine("Blue Top", "Rs. 5000", "2", TOTAL),
                new AutoExOrderLine("Blue Top", "Rs. 500", "20", TOTAL),
                new AutoExOrderLine("Blue Top", "Rs. 500", "2", "Rs. 10000"));
    }

    @Test
    void downloadedInvoiceBelongsToThisCustomerAndThisPurchaseAmount() {
        var assertions = AutoExInvoiceAssertions.validate(CUSTOMER.displayName(), TOTAL, "invoice.txt",
                "Hi ATF Customer,\nYour total purchase amount is 1000.00. Thank you for purchasing.");
        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
    }

    @Test
    void invoiceGreetingAndAmountMayShareTheSameLine() {
        assertTrue(AutoExInvoiceAssertions.validate(CUSTOMER.displayName(), TOTAL, "invoice.txt",
                "Hi ATF Customer, Your total purchase amount is 1000. Thank you")
                .stream().allMatch(AtfAssertion::passed));
    }

    @Test
    void invoiceForAnotherCustomerOrAmountIsRejectedEvenWhenTheFileExists() {
        var assertions = AutoExInvoiceAssertions.validate(CUSTOMER.displayName(), TOTAL, "invoice.txt",
                "Hi ATF Customer2,\nYour total purchase amount is 10000. Thank you for purchasing.");
        assertFalse(assertions.get(1).passed());
        assertFalse(assertions.get(2).passed());
    }

    @Test
    void emptyDownloadOrAnHtmlErrorPageCannotPassAsAnInvoice() {
        assertTrue(AutoExInvoiceAssertions.validate(CUSTOMER.displayName(), TOTAL, "error.html", "")
                .stream().noneMatch(AtfAssertion::passed));
    }

    @Test
    void hiddenAddToCartTextCannotCompleteTheCartCheckpoint() {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = browser.page();
            page.setContent("<div id='cartModal' style='display:none'>Added! Product added to cart</div>");
            var sequence = browser.newActionSequence();
            var session = (AutoExPurchaseSession)
                    Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{AutoExPurchaseSession.class},
                            (object, method, args) -> {
                                String selector = (String) args[0];
                                var target = new AtfLocator(page.locator(selector), selector);
                                if (method.getName().equals("waitForText")) {
                                    sequence.waitForText(page, target, (String) args[1], (String) args[2]);
                                    return null;
                                }
                                if (method.getName().equals("text")) {
                                    return sequence.getText(target, (String) args[1]);
                                }
                                throw new UnsupportedOperationException(method.getName());
                            });
            page.evaluate("() => setTimeout(() => document.getElementById('cartModal').style.display='block', 100)");
            String confirmation = new ProductPage(session).addConfirmation();
            assertTrue(page.locator("div[id='cartModal']").isVisible());
            assertTrue(confirmation.contains("Added!"));
            assertTrue(sequence.snapshot().getFirst().operation().equals("Wait for text"),
                    "The checkpoint must await visible business confirmation, not hidden template text");
        }
    }
}
