package atf.shop.tests.api.unit.exchange;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.testkit.AtfUnitTest;
import atf.shop.support.api.validation.ShopApiValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@AtfUnitTest
class ShopApiValidationResultTest {
    @Test
    void cartAssertionsRemainBoundToTheExchangeThatOwnsTheirContract() {
        var add = evidence("POST", "/carts/1");
        var read = evidence("GET", "/carts/1");
        var assertions = List.of(
                assertion("add request", true), assertion("add response", false),
                assertion("cart read", true), assertion("trace add", true),
                assertion("trace read", true));

        UiApiValidationEvidence result = ShopApiValidationResult.cart(
                List.of(add, read), assertions).reportingEvidence();

        assertEquals(2, result.exchanges().size());
        assertSame(add, result.exchanges().get(0).evidence());
        assertEquals(List.of("add request", "add response"), names(result.exchanges().get(0).assertions()));
        assertEquals(List.of("cart read"), names(result.exchanges().get(1).assertions()));
        assertEquals(List.of("trace add", "trace read"), names(result.assertions()));
        assertEquals("FAIL", result.exchanges().get(0).status().name());
        assertEquals("PASS", result.exchanges().get(1).status().name());
        assertEquals("FAIL", result.status().name());
    }

    @Test
    void orderCorrelationIsNotMisreportedAsAnExchangeAssertion() {
        var order = evidence("POST", "/invoices");
        var assertions = List.of(assertion("created", true), assertion("identity", true),
                assertion("UI/API correlation", false), assertion("total", true),
                assertion("trace", true));

        UiApiValidationEvidence result = ShopApiValidationResult.order(
                List.of(order), assertions).reportingEvidence();

        assertEquals(List.of("UI/API correlation"), names(result.assertions()));
        assertEquals(List.of("created", "identity", "total", "trace"),
                names(result.exchanges().getFirst().assertions()));
        assertEquals("FAIL", result.status().name());
    }

    @Test
    void anExpectedHttp401CanKeepAnExplicitPassingContractVerdict() {
        var unauthorized = evidence("POST", "/users/login", 401);
        var contractPass = assertion("unauthorized credentials are rejected", true);

        var result = new UiApiValidationEvidence(List.of(),
                List.of(new UiApiValidationEvidence.Exchange(unauthorized, List.of(contractPass))));

        assertEquals("PASS", result.exchanges().getFirst().status().name());
        assertEquals("PASS", result.status().name());
    }

    @Test
    void catalogAndPaymentChecksFollowTheirExplicitExchangeOwners() {
        var search = evidence("GET", "/products/search");
        var detail = evidence("GET", "/products/p-1");
        var productChecks = List.of(assertion("search status", true), assertion("search query", true),
                assertion("product identity", true), assertion("product headers", true),
                assertion("search trace", true), assertion("detail trace", true));
        var product = ShopApiValidationResult.product(List.of(search, detail), productChecks)
                .reportingEvidence();

        assertEquals(List.of("search status", "search query"),
                names(product.exchanges().get(0).assertions()));
        assertEquals(List.of("product identity", "product headers"),
                names(product.exchanges().get(1).assertions()));
        assertEquals(List.of("search trace", "detail trace"), names(product.assertions()));

        var payment = ShopApiValidationResult.payment(List.of(evidence("POST", "/payment/check")),
                List.of(assertion("payment request", true), assertion("payment response", true),
                        assertion("payment headers", true), assertion("payment trace", true)))
                .reportingEvidence();
        assertEquals(List.of("payment request", "payment response", "payment headers", "payment trace"),
                names(payment.exchanges().getFirst().assertions()));
    }

    private static List<String> names(List<AtfAssertion> assertions) {
        return assertions.stream().map(AtfAssertion::name).toList();
    }

    private static AtfAssertion assertion(String name, boolean passed) {
        return new AtfAssertion(name, "expected", passed ? "expected" : "different", passed);
    }

    private static ReportExecutionEvidence evidence(String method, String path) {
        return evidence(method, path, 200);
    }

    private static ReportExecutionEvidence evidence(String method, String path, int status) {
        var request = new ReportRequest(method, path, Map.of(), Map.of("Accept", "application/json"),
                "none", "{}");
        var response = new ReportResponse(status, "{}", Map.of("Content-Type", "application/json"),
                Duration.ofMillis(2), "https://api.example.test" + path);
        return new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(), List.of(),
                request, ReportTransportProvenance.LIVE_HTTP);
    }
}
