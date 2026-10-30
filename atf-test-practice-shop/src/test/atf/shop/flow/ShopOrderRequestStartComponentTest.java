package atf.shop.flow;

import atf.shop.api.ShopApiRoutes;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.locators.ShopLocators;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.support.validation.ShopPageValidator;
import atf.shop.support.validation.ShopPurchaseScreenValidator;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.order.ShopOrderReportProbe;
import atf.shop.tests.support.order.ShopOrderRequestStartFixture;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkCapturePolicy;
import atf.ui.network.UiNetworkInspector;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.stream.Stream;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Independent old/current server identities, real subscriptions, one production Confirm and no replay. */
@ShopComponentTest
class ShopOrderRequestStartComponentTest {
    @ParameterizedTest(name = "{0} · inspector={1} · current={2} · start observed={3}")
    @MethodSource("submissions")
    void aLateOldResponseCannotOwnTheCurrentConfirm(String route, boolean inspectorAdapter, int currentStatus,
                                                    boolean startObserved) throws Exception {
        try (var fixture = new ShopOrderRequestStartFixture(route, currentStatus);
             var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)
                     .setArgs(List.of("--host-resolver-rules=MAP api.practicesoftwaretesting.com 127.0.0.1", "--no-proxy-server")));
             var page = browser.newPage();
             var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("api.practicesoftwaretesting.com"))
                     .withCapturePolicy(UiNetworkCapturePolicy.browserApi(List.of("/invoices"))))) {
            page.setDefaultTimeout(3000);
            var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                    ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
            if (startObserved) attach(journal, inspector, page, inspectorAdapter);
            page.exposeFunction("releaseOld", ignored -> { fixture.releaseOld(); return null; });
            page.navigate(fixture.origin() + "/checkout");
            page.setDefaultTimeout(currentStatus == 0 ? 600 : 3000);
            page.evaluate("() => { window.startOld(); }");
            page.waitForCondition(() -> fixture.oldRequests() == 1);
            if (!startObserved) attach(journal, inspector, page, inspectorAdapter);
            assertEquals(0L, journal.exchanges().stream().filter(e -> e.method().equals("POST")).count(),
                    "The old request has started but its response is still blocked");
            var locators = new ShopLocators();
            var actions = new PlaywrightActionSequence();
            var config = ShopConfigurationLoader.load();
            var expected = ShopOrderExpectation.fromUi("12.01", 2);
            var inputs = new ShopPurchaseJourneyContext.CheckoutInputs(page, actions,
                    java.net.URI.create(fixture.origin() + "/checkout"), config.productName(), config.checkout(),
                    new ShopPurchaseCheckpointPublisher(ignored -> { }), journal, locators,
                    new ShopPageValidator(locators), new ShopPurchaseScreenValidator(locators), config.api().endpoint().resolve(atf.shop.api.ShopApiRoutes.POSTCODE_LOOKUP));
            var checkpoint = new ShopCheckoutPurchaseStage(inputs, 0L, expected).confirmOrder().checkpoint();
            page.waitForCondition(() -> Boolean.TRUE.equals(page.evaluate("window.oldDone")));
            assertEquals(currentStatus == 201, checkpoint.passed(), "Only the current successful POST may pass");
            assertEquals(1, ((Number) page.evaluate("window.clicks")).intValue(), "One Confirm click");
            assertEquals(1, fixture.oldRequests(), "Exactly one earlier request remains in flight before the current Confirm");
            assertEquals(currentStatus == 0 ? 0 : 1, fixture.currentRequests(), "No replay or additional order dispatch");
            assertEquals(currentStatus == 0 ? 1L : 2L, journal.exchanges().stream().filter(e -> e.method().equals("POST")).count(),
                    "The old response remains in the raw trace");
            var selected = journal.evidence(ShopApiCheckpoint.ORDER);
            if (currentStatus == 0) {
                assertTrue(selected.isEmpty(), "An old in-flight response cannot become current order evidence");
                assertTrue(journal.selectedEvidence().isEmpty(), "No selected evidence is published when only the old order response exists");
            } else {
                assertEquals(1, selected.size(), "The current Confirm owns one canonical order exchange");
                assertEquals(currentStatus, selected.getFirst().response().status(), "Selected order evidence retains the current HTTP status, including failure");
                assertTrue(selected.getFirst().response().body().contains(currentStatus == 201 ? "current-order" : "current order failed"),
                        "Selected response content belongs to the current submission rather than the late old success");
            }
            if (inspectorAdapter && startObserved && currentStatus == 0 && route.equals(ShopApiRoutes.GUEST_INVOICE_CREATE)) {
                try (var report = browser.newPage()) {
                    report.setDefaultTimeout(3000);
                    for (boolean fallback : List.of(false, true)) {
                        ShopOrderReportProbe.verify(report, page, checkpoint, actions, fallback, "request-start-missing");
                    }
                }
            }
        }
    }

    static Stream<Arguments> submissions() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE)
                .flatMap(route -> Stream.of(false, true).flatMap(adapter -> Stream.concat(
                        Stream.of(0, 201, 500).map(status -> Arguments.of(route, adapter, status, true)),
                        Stream.of(Arguments.of(route, adapter, 0, false)))));
    }

    private static void attach(ShopApiNetworkJournal journal, UiNetworkInspector inspector,
                               com.microsoft.playwright.Page page, boolean inspectorAdapter) {
        if (inspectorAdapter) { journal.attach(inspector); inspector.attach(page); }
        else journal.attach(page);
    }
}
