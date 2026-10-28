package atf.autoex.tests.component.purchase;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.CartPage;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.ui.AutoExUiActions;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

/** Real DOM timing proves bounded readiness without retries or a weakened unique-click guard. */
@AutoExComponentTest
class AutoExCheckoutReadinessComponentTest {
    @Test
    void delayedCheckoutBecomesVisibleBeforeExactlyOneClick() {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = browser.page();
            page.setDefaultTimeout(2_000);
            page.setContent("""
                    <script>window.checkoutClicks=0;setTimeout(()=>{
                      const a=document.createElement('a');a.className='check_out';a.href='#checkout';
                      a.textContent='Proceed to checkout';a.onclick=()=>window.checkoutClicks++;
                      document.body.append(a);
                    },500);</script>
                    """);
            var actions = browser.newActionSequence();
            assertEquals(0, page.locator(AutoExSelectors.CHECKOUT).count());
            new CartPage(port(browser, actions)).proceedToCheckout();
            assertEquals(1, ((Number) page.evaluate("window.checkoutClicks")).intValue());
            assertEquals(List.of("Wait for visible", "Click"), actions.snapshot().stream().map(a -> a.operation()).toList());
        }
    }

    @Test
    void absentOrAmbiguousCheckoutNeverStartsTheClick() {
        for (String markup : List.of("<body></body>",
                "<a class='check_out'>Checkout one</a><a class='check_out'>Checkout two</a>")) {
            try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
                browser.page().setDefaultTimeout(60);
                browser.page().setContent(markup);
                var actions = browser.newActionSequence();
                var cart = new CartPage(port(browser, actions));
                assertThrows(RuntimeException.class, cart::proceedToCheckout);
                assertEquals(List.of("Wait for visible"), actions.snapshot().stream().map(a -> a.operation()).toList());
            }
        }
    }

    private static AutoExUiActions port(PlaywrightUiSession browser, atf.ui.playwright.PlaywrightActionSequence actions) {
        return (AutoExUiActions) Proxy.newProxyInstance(AutoExUiActions.class.getClassLoader(),
                new Class<?>[]{AutoExUiActions.class}, (proxy, method, args) -> {
                    var target = new AtfLocator(browser.page().locator((String) args[0]), (String) args[0]);
                    switch (method.getName()) {
                        case "waitForVisible" -> actions.waitForVisible(target);
                        case "click" -> actions.click(target, (String) args[1]);
                        default -> throw new AssertionError("Unexpected capability: " + method.getName());
                    }
                    return null;
                });
    }
}
