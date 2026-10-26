package atf.shop.tests.api.component.reporting;

import atf.core.reporting.console.AtfConsoleSink;
import atf.console.junit.AtfConsoleTestContext;
import atf.core.http.request.HttpMethod;
import atf.core.http.response.ApiExecutionResult;
import atf.testkit.AtfComponentTest;
import atf.testkit.AtfConsoleMessages;
import atf.console.junit.AtfConsoleTestExtension;
import atf.shop.api.ShopApiClient;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.ShopApiScreenFlow;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.support.reporting.ShopApiReportPublisher;
import atf.shop.tests.api.support.ShopApiReportMetadataFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.support.ShopReportHtmlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static atf.shop.tests.api.support.ShopApiScenarioFixture.COUNTRY;
import static atf.shop.tests.api.support.ShopApiScenarioFixture.HOUSE_NUMBER;
import static atf.shop.tests.api.support.ShopApiScenarioFixture.POSTCODE;
import static atf.shop.tests.api.support.ShopApiScenarioFixture.PRODUCT_NAME;
import static atf.shop.tests.api.support.ShopApiScenarioFixture.QUANTITY;

/** Protects Practice Shop operation mapping through the standalone API report adapter. */
@AtfComponentTest
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@ExtendWith(AtfConsoleTestExtension.class)
class ShopApiContractReportComponentTest {
    @Test
    @DisplayName("Practice Shop API screen flow publishes standalone Extent evidence")
    void publishesStandaloneApiEvidence() throws Exception {
        ShopApiClient client = ShopApiComponentFixture.client();
        ShopApiScreenFlow flow = new ShopApiScreenFlow(client);
        var login = flow.login().establish(ShopApiScenarioFixture.validCredentials());
        var catalog = flow.catalog().openProduct(PRODUCT_NAME);
        var cart = flow.cart().prepare(catalog.productId(), QUANTITY);
        var checkout = flow.checkout().confirm(COUNTRY, POSTCODE, HOUSE_NUMBER,
                ShopPaymentMethod.CASH_ON_DELIVERY);

        List<ApiExecutionResult> executions = List.of(
                login.loginResult(), catalog.search(), catalog.detail(), cart.create(), cart.add(), cart.read(),
                checkout.addressLookup(), checkout.payment());
        assertWriteRequestsHaveJson(executions);
        assertResponsesHaveJson(executions);

        var configuration = ShopConfigurationLoader.load();
        Path reportPath = configuration.apiReportDirectory().resolve(
                "api-atf-practice-shop-standalone_" + Instant.now().toString()
                        .replace(":", "-") + ".html");
        AtfConsoleSink console = AtfConsoleTestContext.configuredSink();
        Path report = ShopApiReportPublisher.publishStandalone(reportPath,
                ShopApiReportMetadataFixture.COMPONENT_TEST_CLASS,
                ShopApiReportMetadataFixture.COMPONENT_TEST_PACKAGE,
                ShopApiReportMetadataFixture.COMPONENT_SCENARIO,
                ShopApiReportMetadataFixture.COMPONENT_TAGS,
                executions, console);

        String html = Files.readString(report);
        assertTrue(Files.isRegularFile(report));
        ShopReportHtmlTestSupport.requireText(html, ShopApiRoutes.PRODUCT_SEARCH,
                "catalog route");
        ShopReportHtmlTestSupport.requireText(html, ShopApiRoutes.PAYMENT_CHECK,
                "payment route");
        ShopReportHtmlTestSupport.requireInOrder(html, List.of(
                "POST /users/login — establish API session",
                "GET /products/search — find configured product",
                "GET /products/01product — read selected product",
                "POST /carts/01cart — add product to cart",
                "GET /carts/01cart — verify cart contents",
                "GET /postcode-lookup — resolve checkout address",
                "POST /payment/check — validate payment selection",
                "TRACE — summarize unique request signatures"),
                "business request order followed by its trace summary");
        ShopReportHtmlTestSupport.requireAbsent(html, "QUERY /products/search",
                "semantic operation vocabulary must not regress");
        ShopReportHtmlTestSupport.requireAbsent(html, "API diagnostics — unique request signatures",
                "trace summary must follow the API action-title vocabulary");
    }

    private static void assertWriteRequestsHaveJson(List<ApiExecutionResult> executions) {
        for (ApiExecutionResult execution : executions) {
            var request = execution.request();
            String method = request.method();
            boolean writeRequest = HttpMethod.POST.value().equalsIgnoreCase(method)
                    || HttpMethod.PUT.value().equalsIgnoreCase(method);
            if (writeRequest) {
                assertFalse(request.bodyText().isBlank(),
                        "POST/PUT must carry a JSON request body: " + request.path());
            }
        }
    }

    private static void assertResponsesHaveJson(List<ApiExecutionResult> executions) {
        for (ApiExecutionResult execution : executions) {
            var request = execution.request();
            var response = execution.response();
            assertFalse(response.body().isBlank(),
                    "API response body must be present for report evidence: " + request.path());
        }
    }

}
