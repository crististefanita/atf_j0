package atf.shop.flow;

import atf.reporting.contract.AtfReportStatus;
import atf.shop.api.ShopApiRoutes;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopProductPage;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopPageValidator;
import atf.shop.support.validation.ShopPurchaseScreenValidator;
import atf.shop.tests.support.ShopComponentTest;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.UiNetworkCapturePolicy;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Real UI adapter and captured guest/authenticated order, with a pre-payment immutable oracle. */
@ShopComponentTest
class ShopCheckoutOrderCorrelationComponentTest {
    @ParameterizedTest(name = "Cart monetary gate · {0}")
    @ValueSource(strings = {"unit", "line", "total"})
    void changedCartAmountsStopBeforeCheckout(String changedAmount) throws Exception {
        try (var fixture = new OrderFixture(ShopApiRoutes.GUEST_INVOICE_CREATE, OrderCase.EXACT, changedAmount);
             var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage()) {
            page.setDefaultTimeout(3000);
            page.navigate(fixture.origin() + "/checkout");
            var actions = new PlaywrightActionSequence();
            var locators = new ShopLocators();
            var expected = new ShopProductPage(page, locators, actions).orderExpectation(2);
            var configuration = ShopConfigurationLoader.load();
            var inputs = new ShopPurchaseJourneyContext.CheckoutInputs(page, actions,
                    java.net.URI.create(fixture.origin() + "/checkout"), configuration.productName(),
                    configuration.checkout(), new ShopPurchaseCheckpointPublisher(ignored -> { }),
                    new ShopApiNetworkJournal(ShopValidationProfile.UI_ONLY),
                    locators, new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators));
            var stage = new ShopCheckoutPurchaseStage(inputs, 0L, expected);
            var failure = assertThrows(IllegalStateException.class,
                    () -> new ShopCheckoutFlow().complete(stage, ignored -> { }, false));
            assertTrue(failure.getMessage().contains("Page validation failed"));
            assertEquals(0, ((Number) page.evaluate("window.proceeds")).intValue(), "Cart failure prevents checkout mutation");
            assertEquals(0, fixture.requests.get(), "No order or payment after a monetary mismatch");
        }
    }

    @ParameterizedTest(name = "{0} · {1}")
    @MethodSource("orders")
    void oneConfirmationUsesTheOriginalPriceAndExactInvoice(String route, OrderCase variant) throws Exception {
        try (var fixture = new OrderFixture(route, variant);
             var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage();
             var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("127.0.0.1"))
                     .withCapturePolicy(UiNetworkCapturePolicy.browserApi(List.of("/invoices"))))) {
            page.setDefaultTimeout(3000);
            var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                    ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
            journal.attach(inspector);
            inspector.attach(page);
            page.navigate(fixture.origin() + "/checkout");
            var locators = new ShopLocators();
            var actions = new PlaywrightActionSequence();
            var expected = new ShopProductPage(page, locators, actions).orderExpectation(2);
            page.evaluate("document.querySelector('[data-test=\"unit-price\"]').textContent='999.99'");
            var configuration = ShopConfigurationLoader.load();
            var inputs = new ShopPurchaseJourneyContext.CheckoutInputs(page, actions,
                    configuration.uri(atf.shop.routes.ShopUiRoutes.CHECKOUT), configuration.productName(),
                    configuration.checkout(), new ShopPurchaseCheckpointPublisher(ignored -> { }), journal,
                    locators, new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators));

            ShopPurchaseCheckpoint checkpoint = new ShopCheckoutPurchaseStage(inputs, 0L, expected).confirmOrder().checkpoint();

            assertEquals(variant == OrderCase.EXACT, checkpoint.passed(), "Current UI and API must agree with the original purchase");
            assertEquals(1, fixture.requests.get(), "Exactly one order dispatch");
            assertEquals(1, ((Number) page.evaluate("window.clicks")).intValue(), "Exactly one Confirm click");
            assertEquals(List.of(route), journal.evidence(ShopApiCheckpoint.ORDER).stream().map(e -> e.request().path()).toList());
            var total = checkpoint.apiValidation().exchanges().getFirst().assertions().stream()
                    .filter(row -> row.name().equals("API response JSON · order total")).findFirst().orElseThrow();
            assertEquals("24.02", total.expected(), "Original product price and configured quantity reach the audit model");
            assertEquals(variant == OrderCase.WRONG_TOTAL ? "0.01" : "24.02", total.actual());
            assertEquals(variant == OrderCase.EXACT ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                    checkpoint.apiValidation().status(), "The checkpoint owns its API verdict");
        }
    }

    static Stream<Arguments> orders() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE)
                .flatMap(route -> Stream.of(OrderCase.values()).map(variant -> Arguments.of(route, variant)));
    }

    enum OrderCase { EXACT, WRONG_TOTAL, INVOICE_PREFIX }

    private static final class OrderFixture implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger requests = new AtomicInteger();

        OrderFixture(String route, OrderCase variant) throws Exception {
            this(route, variant, "");
        }

        OrderFixture(String route, OrderCase variant, String changedAmount) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/checkout", exchange -> {
                String invoice = variant == OrderCase.INVOICE_PREFIX ? "INV-123" : "INV-12";
                String html = "<html><body>CART<span data-test='unit-price'>12.01</span>"
                        + "<span data-test='product-title'>Pliers</span><input data-test='product-quantity' type='number' min='1' value='2'>"
                        + "<span data-test='product-price'>$" + (changedAmount.equals("unit") ? "13.01" : "12.01") + "</span>"
                        + "<span data-test='line-price'>$" + (changedAmount.equals("line") ? "24.03" : "24.02") + "</span>"
                        + "<span data-test='cart-total'>$" + (changedAmount.equals("total") ? "24.03" : "24.02") + "</span>"
                        + "<button data-test='proceed-1' onclick='window.proceeds++'>Proceed to checkout</button>"
                        + "<button onclick=\"confirmOrder()\">Confirm</button>"
                        + "<div id='confirmation'></div><script>window.clicks=0;window.proceeds=0;async function confirmOrder(){window.clicks++;"
                        + "await (await fetch('" + route + "',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'})).text();"
                        + "document.getElementById('confirmation').textContent='Thanks for '+'your order! Your invoice number is "
                        + invoice + ".';}</script></body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.createContext(route, exchange -> {
                requests.incrementAndGet();
                String total = variant == OrderCase.WRONG_TOTAL ? "0.01" : "24.02";
                byte[] bytes = ("{\"id\":\"order-1\",\"invoice_number\":\"INV-12\",\"total\":" + total + "}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        @Override public void close() { server.stop(0); }
    }
}

