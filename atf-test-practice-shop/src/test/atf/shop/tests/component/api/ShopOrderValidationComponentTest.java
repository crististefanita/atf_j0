package atf.shop.tests.component.api;

import atf.core.http.request.HttpMethod;
import atf.reporting.contract.AtfAssertion;
import atf.shop.api.ShopApiRoutes;
import atf.shop.support.api.ShopApiAssertions;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.support.validation.ShopPurchaseConfirmation;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopOrderValidationComponentTest {
    private static final ShopOrderExpectation EXPECTED = ShopOrderExpectation.fromUi("12.01", 2);

    @ParameterizedTest(name = "{0}")
    @MethodSource("orders")
    void orderCannotDeriveItsOwnExpectedTotal(String label, String body, String uiInvoice, boolean passed) {
        var order = new ShopApiNetworkJournal.Exchange(HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE,
                "https://shop.fixture.test" + ShopApiRoutes.GUEST_INVOICE_CREATE, 200, "{}", body,
                Map.of(), Map.of("Content-Type", "application/json"));
        var assertions = ShopApiAssertions.order(order,
                "Thanks for your order! Your invoice number is " + uiInvoice + ".", EXPECTED);
        assertEquals(passed, assertions.stream().allMatch(AtfAssertion::passed), label);
        var total = assertions.stream().filter(a -> a.name().equals("API response JSON · order total")).findFirst().orElseThrow();
        assertEquals("24.02", total.expected(), "Expected total comes from the pre-payment snapshot");
        assertEquals(5, assertions.size(), "Exchange and cross-channel mapping remains stable");
    }

    static Stream<Arguments> orders() {
        return Stream.of(
                order("exact amount and invoice", "24.02", "\"INV-12\"", "INV-12", true),
                order("numeric scale is insignificant", "24.0200", "\"INV-12\"", "INV-12", true),
                order("positive wrong total", "0.01", "\"INV-12\"", "INV-12", false),
                order("wrong quantity total", "36.03", "\"INV-12\"", "INV-12", false),
                order("wrong unit price total", "24.04", "\"INV-12\"", "INV-12", false),
                order("total string rejected", "\"24.02\"", "\"INV-12\"", "INV-12", false),
                order("null total rejected", "null", "\"INV-12\"", "INV-12", false),
                order("invoice prefix rejected", "24.02", "\"INV-12\"", "INV-123", false),
                order("invoice suffix rejected", "24.02", "\"INV-12\"", "XINV-12", false),
                order("invoice extension rejected", "24.02", "\"INV-12\"", "INV-12-EXTRA", false),
                order("invoice decimal extension rejected", "24.02", "\"INV-12\"", "INV-12.3", false),
                order("invoice path extension rejected", "24.02", "\"INV-12\"", "INV-12/3", false),
                order("ambiguous invoices rejected", "24.02", "\"INV-12\"", "INV-12 and INV-123", false),
                order("numeric invoice rejected", "24.02", "12", "INV-12", false),
                Arguments.of("missing total", "{\"id\":\"order-1\",\"invoice_number\":\"INV-12\"}", "INV-12", false),
                Arguments.of("malformed body", "{", "INV-12", false),
                Arguments.of("numeric order identity", "{\"id\":42,\"invoice_number\":\"INV-12\",\"total\":24.02}", "INV-12", false),
                order("difference beyond preview", "24.02", "\"" + "INV-" + "A".repeat(260) + "1\"",
                        "INV-" + "A".repeat(260) + "2", false));
    }

    private static Arguments order(String label, String total, String invoice, String uiInvoice, boolean passed) {
        return Arguments.of(label, "{\"id\":\"order-1\",\"invoice_number\":" + invoice + ",\"total\":" + total + "}",
                uiInvoice, passed);
    }

    @Test
    void moneyUsesExactDecimalsWithoutInventingRoundingOrCurrencyConversion() {
        assertEquals(new BigDecimal("0.30"), ShopOrderExpectation.fromUi("0.10", 3).total());
        assertTrue(EXPECTED.matchesDisplayedTotal("$24.02"));
        assertFalse(EXPECTED.matchesDisplayedTotal("$24.03"));
        assertFalse(EXPECTED.matchesDisplayedTotal("€24.02"));
        assertFalse(EXPECTED.matchesDisplayedUnitPrice("$12.02"));
        for (String price : List.of("0", "-1.00", "12,01", "1,200.00", "NaN", "1e2", "12.001")) {
            assertThrows(IllegalArgumentException.class, () -> ShopOrderExpectation.fromUi(price, 2), price);
        }
        assertThrows(IllegalArgumentException.class, () -> ShopOrderExpectation.fromUi("12.01", 0));
        assertThrows(ArithmeticException.class, () -> new ShopOrderExpectation(new BigDecimal("12.001"), 2));
        assertEquals("", ShopPurchaseConfirmation.invoiceNumber("Thanks for your order! INV-"));
    }
}

