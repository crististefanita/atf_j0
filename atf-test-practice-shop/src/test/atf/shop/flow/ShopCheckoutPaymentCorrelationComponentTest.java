package atf.shop.flow;

import atf.shop.support.validation.ShopOrderExpectation;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.ui.UiExtentReportSession;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.locators.ShopLocators;
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
import org.junit.jupiter.params.provider.EnumSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Exercises the real checkout adapter with one browser click and loopback HTTP, including stale success UI. */
@ShopComponentTest
class ShopCheckoutPaymentCorrelationComponentTest {
    private static final String SECRET = "payment-private-token-must-not-leak";
    private static final String BODY = "{\"message\":\"Payment was successful\",\"context\":\"" + "safe-context ".repeat(35)
            + "\",\"authorization\":\"Bearer " + SECRET + "\"}";

    @ParameterizedTest(name = "Current payment · {0}")
    @EnumSource(PaymentCase.class)
    void oneUiClickCannotValidateAnOlderPaymentResponse(PaymentCase variant) throws Exception {
        int currentStatus = variant.status;
        try (var fixture = new PaymentFixture(currentStatus);
             var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage();
             var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("127.0.0.1"))
                     .withCapturePolicy(UiNetworkCapturePolicy.browserApi(List.of("/payment/check"))))) {
            page.setDefaultTimeout(2000);
            var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                    ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "payment"));
            journal.attach(inspector);
            inspector.attach(page);
            page.navigate(fixture.origin() + "/checkout");
            page.evaluate("async () => (await fetch('/payment/check', {method:'POST',headers:{'Content-Type':'application/json'},"
                    + "body:JSON.stringify({payment_method:'cash-on-delivery'})})).text()");
            page.waitForCondition(() -> journal.sequenceMarker() == 1);
            var configuration = ShopConfigurationLoader.load();
            var locators = new ShopLocators();
            var inputs = new ShopPurchaseJourneyContext.CheckoutInputs(page, new PlaywrightActionSequence(),
                    configuration.uri(atf.shop.routes.ShopUiRoutes.CHECKOUT), configuration.productName(),
                    configuration.checkout(), new ShopPurchaseCheckpointPublisher(ignored -> { }), journal,
                    locators, new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators));

            ShopPurchaseCheckpoint checkpoint = new ShopCheckoutPurchaseStage(inputs, 0L, ShopOrderExpectation.fromUi("12.01", 2)).checkPayment().checkpoint();

            assertTrue(checkpoint.uiAssertions().stream().allMatch(AtfAssertion::passed),
                    "The old visible success cannot decide the API verdict");
            assertEquals(currentStatus == 200, checkpoint.passed());
            assertEquals(currentStatus == 200 ? AtfReportStatus.PASS : AtfReportStatus.FAIL, checkpoint.apiValidation().status());
            assertEquals(1, ((Number) page.evaluate("window.clicks")).intValue(), "Exactly one existing UI click");
            assertEquals(currentStatus == 0 ? 1 : 2, fixture.requests.get(), "No extra HTTP request for reporting or recovery");
            assertEquals(currentStatus == 0 ? List.of() : List.of("E2"), checkpoint.apiEvidence().stream()
                    .map(evidence -> evidence.trace().eventId()).toList());
            if (currentStatus != 0) assertEquals(currentStatus, checkpoint.apiEvidence().iterator().next().response().status());

            Path report = publish(checkpoint, variant);
            String html = Files.readString(report);
            assertFalse(html.contains(SECRET), "Full response is redacted before every projection");
            if (currentStatus != 0) assertTrue(html.contains("sequence=2"), "Selected capture order remains auditable");
            assertEquals(currentStatus == 0 ? 1 : 2, fixture.requests.get(), "Publication never replays payment");
        }
    }

    private static Path publish(ShopPurchaseCheckpoint checkpoint, PaymentCase variant) throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "current-payment-evidence", UUID.randomUUID().toString()));
        try (var report = UiExtentReportSession.openConfiguredResolved(directory, "payment-" + variant.status + ".html",
                atf.reporting.console.AtfConsoleSink.noOp())) {
            var scenario = report.startScenario("ShopCurrentPaymentFixture", "atf.shop.flow",
                    "Current payment · " + variant, List.of("component", "controlled-payment", "ui-api"));
            var phase = scenario.startPhase("TEST BODY");
            phase.step(checkpoint.keyword(), checkpoint.title(), "/checkout", "", checkpoint.uiAssertions(), List.of(),
                    checkpoint.apiValidation(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            var verdict = checkpoint.passed() ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            phase.completeFinal(verdict);
            scenario.complete(verdict);
            return report.reportPath();
        }
    }

    enum PaymentCase {
        SUCCESS(200, "HTTP 200"), FAILURE(500, "HTTP 500"), MISSING(0, "No new request");
        private final int status;
        private final String label;
        PaymentCase(int status, String label) { this.status = status; this.label = label; }
        @Override public String toString() { return label; }
    }

    private static final class PaymentFixture implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger requests = new AtomicInteger();

        PaymentFixture(int currentStatus) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/payment/check", exchange -> {
                int responseStatus = requests.incrementAndGet() == 1 ? 200 : currentStatus;
                byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.createContext("/checkout", exchange -> {
                String action = currentStatus == 0 ? "" : "fetch('/payment/check',{method:'POST',headers:{'Content-Type':'application/json'},"
                        + "body:JSON.stringify({payment_method:'cash-on-delivery'})});";
                byte[] html = ("<button data-test='finish'>Check payment</button><div>Payment was successful</div>"
                        + "<script>window.clicks=0;document.querySelector('button').onclick=()=>{window.clicks++;" + action + "};</script>")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, html.length);
                try (var output = exchange.getResponseBody()) { output.write(html); }
            });
            server.start();
        }

        String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        @Override public void close() { server.stop(0); }
    }
}
