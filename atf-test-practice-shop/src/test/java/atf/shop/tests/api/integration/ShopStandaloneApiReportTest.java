package atf.shop.tests.api.integration;

import atf.api.console.AtfConsoleReporter;
import atf.api.console.junit.AtfConsoleTestExtension;
import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.response.ApiExecutionResult;
import atf.api.core.http.transport.ApiTransportProvenance;
import atf.api.core.reporting.console.AtfConsoleSink;
import atf.api.testkit.AtfIntegrationTest;
import atf.api.testkit.AtfConsoleMessages;
import atf.shop.api.ShopApiClient;
import atf.shop.api.ShopApiScreenFlow;
import atf.shop.api.ShopProductQuery;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.api.contract.ShopProductSort;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.model.ShopCredentials;
import atf.shop.api.ShopApiSession;
import atf.shop.support.api.ShopApiAssertions;
import atf.shop.support.api.ShopApiExecutionExchange;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.support.reporting.ShopApiReportPublisher;
import atf.shop.support.api.ShopApiReportValidation;
import atf.shop.tests.api.support.ShopApiReportMetadataFixture;
import atf.shop.tests.api.integration.support.ShopApiLiveCartScope;
import atf.shop.tests.support.ShopReportHtmlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import atf.shop.tests.support.ShopTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Authoritative live business artifact. The component report remains the
 * deterministic contract matrix; this test is the only business PASS source.
 */
@Tag("api")
@AtfIntegrationTest
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@ExtendWith(AtfConsoleTestExtension.class)
@ShopTestSupport.RequiresLiveAuthenticatedApi
public class ShopStandaloneApiReportTest {
    private static final String REPORT_PREFIX = "api-atf-practice-shop-standalone_";
    private static final String AUTHENTICATION_PHASE = "authentication";
    private static final String CATALOG_PHASE = "catalog";
    private static final String CART_PHASE = "cart";
    private static final String CHECKOUT_PHASE = "checkout";

    @Test
    @DisplayName("Practice Shop live API business flow publishes real HTTP evidence")
    void publishesLiveApiBusinessEvidence() throws Exception {
        ShopConfiguration configuration = ShopConfigurationLoader.load();
        ShopCredentials credentials = ShopCredentials.load(configuration.accountFile());
        ShopPaymentMethod paymentMethod = ShopPaymentMethod.from(
                configuration.checkout().paymentMethod());
        try (ShopApiClient client = ShopApiClient.live(configuration.api())) {
            ShopApiScreenFlow flow = new ShopApiScreenFlow(client);

            ShopApiSession login = client.login(credentials);
            if (!login.authenticated()) {
                publishLiveLoginFailure(configuration, login, credentials);
                assertTrue(login.authenticated(), "live API login precondition must be authenticated");
                return;
            }

            var catalog = flow.catalog().openProduct(configuration.productName());
            String categoryId = categoryId(catalog.detail().response().body());
            var categoryQuery = new ShopProductQuery(null, categoryId, null, null,
                    ShopProductSort.PRICE_ASC, 1);
            ApiExecutionResult filteredCatalog = client.execute(
                    client.requests().products(categoryQuery));

            int requestedQuantity = configuration.checkout().quantity();
            int updatedQuantity = requestedQuantity + 1;
            ApiExecutionResult cartCreate;
            ApiExecutionResult cartAdd;
            ApiExecutionResult cartRead;
            ApiExecutionResult cartUpdate;
            ApiExecutionResult cartReadAfterUpdate;
            ApiExecutionResult cartRemove;
            ApiExecutionResult cartReadAfterRemoval;
            ApiExecutionResult addressLookup;
            ApiExecutionResult paymentCheck;
            String cartId;
            ShopApiLiveCartScope cart = new ShopApiLiveCartScope(client, catalog.productId());
            try (cart) {
                cartCreate = cart.create();
                cartAdd = cart.add(requestedQuantity);
                cartRead = cart.read();
                cartUpdate = cart.updateQuantity(updatedQuantity);
                cartReadAfterUpdate = cart.read();

                var checkout = flow.checkout().confirm(
                        configuration.checkout().billing().country(),
                        configuration.checkout().billing().postalCode(),
                        configuration.checkout().billing().houseNumber(), paymentMethod);
                addressLookup = checkout.addressLookup();
                paymentCheck = checkout.payment();

                cartRemove = cart.remove();
                cartReadAfterRemoval = cart.read();
                cartId = cart.cartId();
            }
            List<ApiExecutionResult> cleanupExecutions = cart.cleanupExecutions();

            List<ApiExecutionResult> writes = List.of(cartCreate, cartAdd, cartUpdate, cartRemove);
            assertTrue(writes.stream().allMatch(execution -> execution.attempts() == 1),
                    "non-idempotent live cart writes must not be retried");
            assertTrue(ShopApiHttpStatus.OK.matches(filteredCatalog.response().status()),
                    "filtered catalog request must return HTTP 200");
            assertTrue(ShopApiHttpStatus.OK.matches(cartReadAfterRemoval.response().status()),
                    "cart must remain readable after removing the product");

            List<ApiExecutionResult> executions = new ArrayList<>(List.of(
                    login.loginResult(), catalog.search(), catalog.detail(), filteredCatalog,
                    cartCreate, cartAdd, cartRead, cartUpdate, cartReadAfterUpdate,
                    addressLookup, paymentCheck, cartRemove, cartReadAfterRemoval));
            executions.addAll(cleanupExecutions);
            assertTrue(executions.stream().allMatch(execution ->
                            execution.provenance() == ApiTransportProvenance.LIVE_HTTP),
                    "business report must never be built from acceptance/mock transport");

            var loginExchange = ShopApiExecutionExchange.from(
                    login.loginResult(), 1, AUTHENTICATION_PHASE);
            var searchExchange = ShopApiExecutionExchange.from(
                    catalog.search(), 2, CATALOG_PHASE);
            var detailExchange = ShopApiExecutionExchange.from(
                    catalog.detail(), 3, CATALOG_PHASE);
            var filteredCatalogExchange = ShopApiExecutionExchange.from(
                    filteredCatalog, 4, CATALOG_PHASE);
            var addExchange = ShopApiExecutionExchange.from(cartAdd, 6, CART_PHASE);
            var readExchange = ShopApiExecutionExchange.from(cartRead, 7, CART_PHASE);
            var updateExchange = ShopApiExecutionExchange.from(cartUpdate, 8, CART_PHASE);
            var readAfterUpdateExchange = ShopApiExecutionExchange.from(
                    cartReadAfterUpdate, 9, CART_PHASE);
            var removeExchange = ShopApiExecutionExchange.from(cartRemove, 12, CART_PHASE);
            var readAfterRemovalExchange = ShopApiExecutionExchange.from(
                    cartReadAfterRemoval, 13, CART_PHASE);
            var paymentExchange = ShopApiExecutionExchange.from(paymentCheck, 11, CHECKOUT_PHASE);
            List<ShopApiReportValidation> validations = List.of(
                    new ShopApiReportValidation("Authentication business contract",
                            ShopApiAssertions.login(loginExchange)),
                    new ShopApiReportValidation("Catalog business contract",
                            ShopApiAssertions.product(searchExchange, detailExchange,
                                    configuration.productName(), catalog.productId())),
                    new ShopApiReportValidation("Live category and price-sort contract",
                            ShopApiAssertions.categoryProductsSortedByPrice(
                                    filteredCatalogExchange, categoryId)),
                    new ShopApiReportValidation("Cart add business contract",
                            ShopApiAssertions.cart(addExchange, readExchange,
                                    catalog.productId(), requestedQuantity)),
                    new ShopApiReportValidation("Live cart mutation and cleanup contract",
                            ShopApiAssertions.cartQuantityUpdateAndRemoval(updateExchange,
                                    readAfterUpdateExchange, removeExchange,
                                    readAfterRemovalExchange, cartId, catalog.productId(),
                                    updatedQuantity)),
                    new ShopApiReportValidation("Checkout business contract",
                            ShopApiAssertions.payment(paymentExchange, paymentMethod.wireValue())));

            Path reportPath = configuration.apiReportDirectory().resolve(
                    REPORT_PREFIX + Instant.now().toString().replace(":", "-") + ".html");
            AtfConsoleSink console = AtfConsoleReporter.configuredStdout();
            Path report = ShopApiReportPublisher.publishLiveStandalone(reportPath,
                    ShopApiReportMetadataFixture.LIVE_TEST_CLASS,
                    ShopApiReportMetadataFixture.LIVE_TEST_PACKAGE,
                    ShopApiReportMetadataFixture.LIVE_SCENARIO,
                    ShopApiReportMetadataFixture.LIVE_TAGS,
                    executions, validations, console);

            assertTrue(Files.isRegularFile(report), "live API report must be written");
            String html = Files.readString(report);
            ShopReportHtmlTestSupport.requireText(html, "LIVE / HTTP", "live provenance");
            for (String contract : List.of("Authentication business contract",
                    "Catalog business contract", "Live category and price-sort contract",
                    "Cart add business contract", "Live cart mutation and cleanup contract",
                    "Checkout business contract")) {
                ShopReportHtmlTestSupport.requireText(html, contract, "business validation");
            }
            ShopReportHtmlTestSupport.requireText(html, "REQUEST JSON", "request body disclosure");
            ShopReportHtmlTestSupport.requireText(html, "RESPONSE JSON", "response body disclosure");
            assertTrue(html.contains("Payment was successful") || html.contains("payment_method"),
                    "payment business evidence must remain visible");
            ShopReportHtmlTestSupport.requireText(html, HttpMethod.POST.value(),
                    "write operations must remain visible");
            assertTrue(validations.stream().allMatch(ShopApiReportValidation::passed),
                    "all live business contracts must pass; inspect the generated report for evidence");
        }
    }

    private static String categoryId(String productBody) {
        var product = ShopJsonEvidence.parse(productBody);
        String categoryId = product == null ? "" : product.path(ShopApiJsonField.CATEGORY.wireValue())
                .path(ShopApiJsonField.ID.wireValue()).asText("");
        if (categoryId.isBlank()) {
            throw new IllegalStateException("Live product detail did not expose a category id");
        }
        return categoryId;
    }

    private static void publishLiveLoginFailure(ShopConfiguration configuration,
                                                 ShopApiSession login,
                                                 ShopCredentials credentials) throws Exception {
        var exchange = ShopApiExecutionExchange.from(login.loginResult(), 1, AUTHENTICATION_PHASE);
        List<ShopApiReportValidation> validations = List.of(
                new ShopApiReportValidation("Authentication business contract",
                        ShopApiAssertions.login(exchange)));
        Path reportPath = configuration.apiReportDirectory().resolve(
                REPORT_PREFIX + Instant.now().toString().replace(":", "-") + ".html");
        Path report = ShopApiReportPublisher.publishLiveStandalone(reportPath,
                ShopApiReportMetadataFixture.LIVE_TEST_CLASS,
                ShopApiReportMetadataFixture.LIVE_TEST_PACKAGE,
                ShopApiReportMetadataFixture.LIVE_SCENARIO,
                ShopApiReportMetadataFixture.LIVE_TAGS,
                List.of(login.loginResult()), validations, AtfConsoleReporter.configuredStdout());
        assertTrue(Files.isRegularFile(report), "failed live precondition must still publish evidence");
    }
}
