package atf.shop.flow;

import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.tests.support.order.ShopOrderReportProbe;
import atf.core.http.protocol.HttpHeaderProfiles;
import atf.ui.network.UiNetworkObservation;
import java.time.Instant;
import java.util.Map;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;

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
import com.microsoft.playwright.TimeoutError;
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
                    locators, new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators), configuration.api().endpoint().resolve(atf.shop.api.ShopApiRoutes.POSTCODE_LOOKUP));
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
            page.setDefaultTimeout(variant == OrderCase.MISSING_REQUEST ? 500 : 3000);
            var aborted = new AtomicInteger();
            if (variant == OrderCase.REQUEST_FAILED) page.route("**" + route, intercepted -> {
                aborted.incrementAndGet(); intercepted.abort();
            });
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
                    locators, new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators), configuration.api().endpoint().resolve(atf.shop.api.ShopApiRoutes.POSTCODE_LOOKUP));

            var stage = new ShopCheckoutPurchaseStage(inputs, 0L, expected);
            if (variant == OrderCase.HTTP_500_WITHOUT_UI) {
                assertThrows(TimeoutError.class, stage::confirmOrder, "Original UI timeout remains primary");
                assertEquals(500, journal.evidence(ShopApiCheckpoint.ORDER).getFirst().response().status());
                assertEquals(1, fixture.requests.get());
                assertEquals(1, ((Number) page.evaluate("window.clicks")).intValue());
                return;
            }
            ShopPurchaseCheckpoint checkpoint = stage.confirmOrder().checkpoint();

            assertEquals(variant == OrderCase.EXACT, checkpoint.passed(), "Current UI and API must agree with the original purchase");
            int expectedDispatches = variant == OrderCase.MISSING_REQUEST || variant == OrderCase.REQUEST_FAILED ? 0 : 1;
            assertEquals(expectedDispatches, fixture.requests.get(), "No additional server dispatch");
            assertEquals(variant == OrderCase.REQUEST_FAILED ? 1 : 0, aborted.get(), "One observed browser attempt when transport fails");
            assertEquals(1, ((Number) page.evaluate("window.clicks")).intValue(), "Exactly one Confirm click");
            assertEquals(variant == OrderCase.MISSING_REQUEST ? List.of() : List.of(route),
                    journal.evidence(ShopApiCheckpoint.ORDER).stream().map(e -> e.request().path()).toList());
            if (variant == OrderCase.HTTP_500) assertEquals(500, journal.evidence(ShopApiCheckpoint.ORDER).getFirst().response().status());
            if (variant == OrderCase.REQUEST_FAILED) assertEquals("Unavailable", journal.evidence(ShopApiCheckpoint.ORDER).getFirst().response().statusLabel());
            var total = Stream.concat(checkpoint.apiValidation().assertions().stream(),
                    checkpoint.apiValidation().exchanges().stream().flatMap(exchange -> exchange.assertions().stream()))
                    .filter(row -> row.name().equals("API response JSON · order total")).findFirst().orElseThrow();
            assertEquals("24.02", total.expected(), "Original product price and configured quantity reach the audit model");
            String expectedActual = switch (variant) {
                case EXACT, INVOICE_PREFIX -> "24.02";
                case WRONG_TOTAL -> "0.01";
                default -> "missing";
            };
            assertEquals(expectedActual, total.actual());
            assertEquals(variant == OrderCase.EXACT ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                    checkpoint.apiValidation().status(), "The checkpoint owns its API verdict");
        }
    }

    @ParameterizedTest(name = "Current order · {0} · {1}") @MethodSource("failedOrders")
    void currentFailureOrMissingCaptureReplacesTheOldSelectionAfterOneConfirm(String route, OrderFailureCase variant) throws Exception {
        try (var fixture = new OrderProjectionFixture(route);
             var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage(); var report = browser.newPage();
             var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("127.0.0.1"))
                     .withCapturePolicy(UiNetworkCapturePolicy.browserApi(List.of("/invoices"))))) {
            page.setDefaultTimeout(variant == OrderFailureCase.MISSING_CAPTURE ? 500 : 3000); report.setDefaultTimeout(3000);
            var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                    ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
            journal.attach(inspector);
            if (variant != OrderFailureCase.MISSING_CAPTURE) inspector.attach(page);
            var submissions = new AtomicInteger();
            page.onRequest(request -> {
                if (request.method().equals("POST") && request.url().endsWith(route)) submissions.incrementAndGet();
            });
            if (variant == OrderFailureCase.TRANSPORT_FAILURE) page.route("**" + route, intercepted -> intercepted.abort("failed"));
            page.navigate(fixture.origin() + "/checkout");
            inspector.capture(new UiNetworkObservation(1, Instant.EPOCH, "POST", fixture.origin() + route, route,
                    201, Map.of(), HttpHeaderProfiles.JSON_CONTENT, "{}",
                    "{\"id\":\"old-order\",\"invoice_number\":\"INV-12\",\"total\":24.02}",
                    "none", "none", "127.0.0.1", "CONTROLLED_PRIOR_CAPTURE", "", true,
                    "", UiNetworkCapturePolicy.ResourceKind.FETCH).withRequestIdentity(inspector.requestStarted()));
            var expected = ShopOrderExpectation.fromUi("12.01", 2);
            assertTrue(journal.orderCreated(0L, "Thanks for your order! INV-12", expected).stream().allMatch(row -> row.passed()));
            var locators = new ShopLocators(); var actions = new PlaywrightActionSequence();
            var configuration = ShopConfigurationLoader.load();
            var inputs = new ShopPurchaseJourneyContext.CheckoutInputs(page, actions,
                    configuration.uri(atf.shop.routes.ShopUiRoutes.CHECKOUT), configuration.productName(),
                    configuration.checkout(), new ShopPurchaseCheckpointPublisher(ignored -> { }), journal,
                    locators, new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators), configuration.api().endpoint().resolve(atf.shop.api.ShopApiRoutes.POSTCODE_LOOKUP));
            var checkpoint = new ShopCheckoutPurchaseStage(inputs, 0L, expected).confirmOrder().checkpoint();
            assertFalse(checkpoint.passed());
            assertEquals(AtfReportStatus.FAIL, checkpoint.apiValidation().status());
            assertEquals(1, submissions.get(), "Only this Confirm dispatches; the older capture is fixture input");
            assertEquals(variant == OrderFailureCase.TRANSPORT_FAILURE ? 0 : 1, fixture.requests.get(),
                    "A pre-HTTP transport failure never reaches the loopback server");
            assertEquals(1, ((Number) page.evaluate("window.clicks")).intValue());
            assertEquals(variant == OrderFailureCase.MISSING_CAPTURE ? 0 : 1, checkpoint.apiEvidence().size());
            if (variant != OrderFailureCase.MISSING_CAPTURE) {
                assertEquals("E2", checkpoint.apiEvidence().iterator().next().trace().eventId());
                assertEquals(variant == OrderFailureCase.HTTP_FAILURE ? 500 : -1,
                        checkpoint.apiEvidence().iterator().next().response().status());
            }
            for (boolean fallback : List.of(false, true)) ShopOrderReportProbe.verify(report, page, checkpoint,
                    actions, fallback, (route.endsWith("guest") ? "guest-" : "authenticated-") + variant.name().toLowerCase());
        }
    }

    static Stream<Arguments> failedOrders() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE)
                .flatMap(route -> Stream.of(OrderFailureCase.HTTP_FAILURE, OrderFailureCase.MISSING_CAPTURE, OrderFailureCase.TRANSPORT_FAILURE)
                        .map(variant -> Arguments.of(route, variant)));
    }

    enum OrderFailureCase { HTTP_FAILURE, MISSING_CAPTURE, TRANSPORT_FAILURE }

    /** One current POST; an older observation is supplied separately through the public inspector. */
    private static final class OrderProjectionFixture implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger requests = new AtomicInteger();
        OrderProjectionFixture(String route) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/checkout", exchange -> {
                String html = "<button onclick=\"confirmOrder()\">Confirm</button>"
                        + "<div id='confirmation'>Thanks for your order! Your invoice number is INV-12.</div>"
                        + "<script>window.clicks=0;async function confirmOrder(){window.clicks++;"
                        + "try{await (await fetch('" + route + "',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'})).text();}catch(error){window.transportFailure=true;}"
                        + "document.getElementById('confirmation').textContent='Thanks for '+'your order! Your invoice number is INV-12.';}</script>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes); exchange.close();
            });
            server.createContext(route, exchange -> {
                requests.incrementAndGet();
                String body = "{\"message\":\"current order failed\",\"password\":\"raw-order-secret\",\"diagnostic\":\"" + "x".repeat(350) + "\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, bytes.length);
                exchange.getResponseBody().write(bytes); exchange.close();
            });
            server.start();
        }
        String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        @Override public void close() { server.stop(0); }
    }

    static Stream<Arguments> orders() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE)
                .flatMap(route -> Stream.of(OrderCase.values()).map(variant -> Arguments.of(route, variant)));
    }

    enum OrderCase { EXACT, WRONG_TOTAL, INVOICE_PREFIX, HTTP_500, MISSING_REQUEST, REQUEST_FAILED, HTTP_500_WITHOUT_UI }

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
                boolean oldSuccess = variant == OrderCase.HTTP_500 || variant == OrderCase.MISSING_REQUEST || variant == OrderCase.REQUEST_FAILED;
                String seed = oldSuccess ? "Thanks for your order! Your invoice number is INV-12." : "";
                String dispatch = variant == OrderCase.MISSING_REQUEST ? "" : "try {await (await fetch('" + route
                        + "',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'})).text();}catch(error){}";
                String html = "<html><body>CART<span data-test='unit-price'>12.01</span>"
                        + "<span data-test='product-title'>Pliers</span><input data-test='product-quantity' type='number' min='1' value='2'>"
                        + "<span data-test='product-price'>$" + (changedAmount.equals("unit") ? "13.01" : "12.01") + "</span>"
                        + "<span data-test='line-price'>$" + (changedAmount.equals("line") ? "24.03" : "24.02") + "</span>"
                        + "<span data-test='cart-total'>$" + (changedAmount.equals("total") ? "24.03" : "24.02") + "</span>"
                        + "<button data-test='proceed-1' onclick='window.proceeds++'>Proceed to checkout</button>"
                        + "<button onclick=\"confirmOrder()\">Confirm</button>"
                        + "<div id='confirmation'>" + seed + "</div><script>window.clicks=0;window.proceeds=0;async function confirmOrder(){window.clicks++;"
                        + dispatch
                        + (variant == OrderCase.HTTP_500_WITHOUT_UI ? ""
                        : "document.getElementById('confirmation').textContent='Thanks for '+'your order! Your invoice number is " + invoice + ".';")
                        + "}</script></body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.createContext(route, exchange -> {
                requests.incrementAndGet();
                String total = variant == OrderCase.WRONG_TOTAL ? "0.01" : "24.02";
                String body = variant == OrderCase.HTTP_500 || variant == OrderCase.HTTP_500_WITHOUT_UI
                        ? "{\"message\":\"current order rejected\"}"
                        : "{\"id\":\"order-1\",\"invoice_number\":\"INV-12\",\"total\":" + total + "}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(variant == OrderCase.HTTP_500 || variant == OrderCase.HTTP_500_WITHOUT_UI ? 500 : 200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        @Override public void close() { server.stop(0); }
    }
}

