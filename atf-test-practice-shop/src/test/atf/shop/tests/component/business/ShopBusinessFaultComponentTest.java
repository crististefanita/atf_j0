package atf.shop.tests.component.business;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.business.ShopBusinessFault;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Native local DOM proof; no public account or purchase is created by this component. */
@ShopComponentTest
class ShopBusinessFaultComponentTest {
    @ParameterizedTest @EnumSource(ShopBusinessFault.Point.class)
    void faultsOneBusinessBoundaryWithoutChangingTheExpectedValueOrReplayingRequests(ShopBusinessFault.Point point) {
        try (var playwright = Playwright.create(); var browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)); var page = browser.newPage()) {
            var requests = new AtomicInteger();
            page.route("**/*", route -> {
                requests.incrementAndGet();
                route.fulfill(new com.microsoft.playwright.Route.FulfillOptions().setContentType("text/html").setBody("""
                        <h1>My account</h1><table><tr><td data-test="line-price">19.99</td></tr></table>
                        <div>Thanks for your order! INV-001</div>
                        """));
            });
            page.navigate("http://shop-fault.local/account");
            var fault = new ShopBusinessFault(page, point);
            fault.arm();
            var locators = new atf.shop.locators.ShopLocators();
            switch (point) {
                case ACCOUNT -> assertFalse(locators.accountHeading(page).locator().isVisible(), "The actual account visibility gate fails");
                case CART -> assertEquals("0.01", locators.control(page, atf.shop.locators.ShopControl.CART_LINE_TOTAL).locator().textContent(), "The cart expected total remains independent");
                case PURCHASE -> {
                    assertTrue(locators.pageBody(page).locator().textContent().contains("Thanks for your order!"), "The real native confirmation wait remains satisfied");
                    assertFalse(locators.pageBody(page).locator().textContent().contains("INV-001"), "Only the invoice business oracle is broken");
                }
                case AFTER_SETUP -> {
                    assertTrue(locators.accountHeading(page).locator().isVisible(), "The account observation remains healthy before the generic error");
                    assertEquals(0, fault.injections(), "Arming alone cannot invent a post-PASS failure");
                    var failure = assertThrows(IllegalStateException.class, fault::afterSetup, "The explicit post-checkpoint boundary fails once");
                    assertEquals(ShopBusinessFault.AFTER_SETUP_MESSAGE, failure.getMessage(), "The exact generic diagnostic is preserved");
                    fault.afterSetup();
                    assertTrue(locators.accountHeading(page).locator().isVisible(), "Generic failure does not corrupt an earlier PASS observation");
                }
            }
            fault.arm();
            assertEquals(1, fault.injections(), "The fault is applied exactly once in this browser context");
            assertEquals(1, ((Number) page.evaluate("() => document.querySelectorAll('#atf-business-fault').length")).intValue(), "The original failure gets one visible capture marker");
            assertEquals(1, requests.get(), "The fixture dispatches no additional request or mutation");
        }
    }
}
