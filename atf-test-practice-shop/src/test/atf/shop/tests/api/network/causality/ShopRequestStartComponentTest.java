package atf.shop.tests.api.network.causality;

import atf.shop.api.ShopApiRoutes;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.*;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.tests.support.ShopComponentTest;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.*;
import atf.ui.network.identity.UiRequestIdentity;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import atf.shop.locators.ShopLocators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopRequestStartComponentTest {
    private static final ShopOrderExpectation EXPECTED = ShopOrderExpectation.fromUi("12.01", 2);
    private static final String CONFIRMATION = "Thanks for your order! Your invoice number is INV-12.";

    @ParameterizedTest(name = "{0} · inspector={1} · current={2}") @MethodSource("delayedOrders")
    void legacyFencesAlsoExcludeTheEarlierRequestEvenWhenItsResponseArrivesAfterConfirm(String route, boolean generic, int currentStatus) throws Exception {
        try (var fixture = new DelayedOrderFixture(route, currentStatus); var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true).setArgs(List.of(
                     "--host-resolver-rules=MAP api.practicesoftwaretesting.com 127.0.0.1", "--no-proxy-server")));
             var page = browser.newPage(); var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(
                     List.of("api.practicesoftwaretesting.com")).withCapturePolicy(UiNetworkCapturePolicy.browserApi(List.of("/invoices"))))) {
            page.setDefaultTimeout(3000); var journal = journal();
            if (generic) { journal.attach(inspector); inspector.attach(page); } else journal.attach(page);
            page.navigate(fixture.origin() + "/page", new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(10_000));
            page.evaluate("route => { window.previousOrder=fetch(route,{method:'POST',body:'{}'}).then(r=>r.text()); }", route);
            assertTrue(fixture.oldReceived.await(3, TimeUnit.SECONDS));
            long marker = journal.sequenceMarker();
            new ShopLocators().confirmOrder(page).locator().click(); fixture.release.countDown();
            page.evaluate("async () => { await window.previousOrder; if(window.currentOrder) await window.currentOrder; }");
            page.waitForCondition(() -> journal.exchanges().stream().filter(exchange -> exchange.method().equals("POST")
                    && exchange.path().equals(route)).count() == (currentStatus == 0 ? 1 : 2));
            var checks = journal.orderCreated(marker, CONFIRMATION, EXPECTED);
            assertEquals(currentStatus == 201, checks.stream().allMatch(row -> row.passed()));
            assertEquals(currentStatus == 0 ? 1 : 2, fixture.dispatches.get()); assertEquals(1, ((Number) page.evaluate("window.confirms")).intValue());
            var selected = journal.evidence(ShopApiCheckpoint.ORDER);
            if (currentStatus == 0) assertTrue(selected.isEmpty());
            else { assertEquals(1, selected.size()); assertEquals(currentStatus, selected.getFirst().response().status());
                assertTrue(selected.getFirst().response().body().contains("current-order")); }
        }
    }
    static Stream<Arguments> delayedOrders() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE).flatMap(route ->
                Stream.of(false, true).flatMap(generic -> Stream.of(0, 201, 500).map(status -> Arguments.of(route, generic, status))));
    }

    @Test void unknownOrForeignStartIdentityCannotProveTheTypedCurrentOrderWindow() {
        var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("shop.fixture.test")));
        var journal = journal(); journal.attach(inspector); var marker = journal.requestMarker();
        var raw = new UiNetworkObservation(900, Instant.EPOCH, "POST", "https://shop.fixture.test/invoices", "/invoices", 201,
                Map.of(), Map.of("content-type", "application/json"), "{}", "{\"id\":\"current-order\",\"invoice_number\":\"INV-12\",\"total\":24.02}",
                "none", "none", "fixture", "controlled", "untrusted", true, "", UiNetworkCapturePolicy.ResourceKind.FETCH);
        inspector.capture(raw); assertFalse(journal.orderCreated(marker, CONFIRMATION, EXPECTED).stream().allMatch(row -> row.passed()));
        inspector.capture(raw.withRequestIdentity(new UiRequestIdentity(UUID.randomUUID(), 500)));
        assertFalse(journal.orderCreated(marker, CONFIRMATION, EXPECTED).stream().allMatch(row -> row.passed()));
        assertTrue(journal.evidence(ShopApiCheckpoint.ORDER).isEmpty());
        inspector.capture(raw.withRequestIdentity(inspector.requestStarted()));
        assertTrue(journal.orderCreated(marker, CONFIRMATION, EXPECTED).stream().allMatch(row -> row.passed()));
    }
    private static ShopApiNetworkJournal journal() {
        return new ShopApiNetworkJournal(ShopValidationProfile.UI_API, ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
    }
}
