package atf.autoex.tests.support.identity;

import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.flow.cart.AutoExCheckoutUiFlow;
import atf.autoex.page.AccountPage;
import atf.autoex.page.CartPage;
import atf.autoex.runtime.account.AutoExCustomerIdentityObservation;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Route;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;

/** Local browser fixture exposes a business adapter; executable component tests never own browser mechanics. */
public final class AutoExCustomerIdentityFixture implements AutoCloseable {
    private static final Catalog PRODUCT = new Catalog("Blue Top", "1", "Blue Top", "Rs. 500", 2);
    private final PlaywrightUiSession browser;
    private final AutoExCheckoutUiFlow adapter;

    public AutoExCustomerIdentityFixture(String mode) {
        URI base = URI.create("http://identity-fixture.test");
        browser = PlaywrightUiSession.open(new UiBrowserConfiguration(base, "chromium", true,
                Duration.ofMillis(600), Duration.ofSeconds(5), 1000, 800, 100, true));
        try {
            var page = browser.page();
            page.route(base + "/**", route -> route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html").setBody(document(mode))));
            page.navigate(base + "/cart");
            var actions = browser.newActionSequence();
            var observation = new AutoExCustomerIdentityObservation(page, actions);
            var port = (AutoExAccountPagePort) Proxy.newProxyInstance(AutoExAccountPagePort.class.getClassLoader(),
                    new Class<?>[]{AutoExAccountPagePort.class}, (instance, method, args) -> switch (method.getName()) {
                        case "observeCustomerName" -> observation.read();
                        case "text" -> actions.getText(new AtfLocator(page.locator((String) args[0]), (String) args[0]), (String) args[1]);
                        case "visible" -> page.locator((String) args[0]).isVisible();
                        case "waitForVisible" -> { actions.waitForVisible(new AtfLocator(page.locator((String) args[0]), (String) args[0])); yield null; }
                        case "click" -> { actions.click(new AtfLocator(page.locator((String) args[0]), (String) args[0]), (String) args[1]); yield null; }
                        default -> throw new AssertionError("Unexpected page operation: " + method.getName());
                    });
            adapter = new AutoExCheckoutUiFlow(new CartPage(port), new AccountPage(port), PRODUCT);
        } catch (RuntimeException | Error failure) {
            try { browser.close(); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public AutoExCheckoutUiFlow adapter() { return adapter; }
    public String checkoutActivations() { return String.valueOf(browser.page().evaluate("String(window.checkouts||0)")); }
    @Override public void close() { browser.close(); }

    private static String document(String mode) {
        String identity = mode.equals("foreign") ? "foreign customer" : mode.equals("empty") ? "" : "owned customer";
        String header = mode.equals("absent") ? "" : "<a><i class='fa-user'></i><b>" + identity + "</b></a>";
        if (mode.equals("ambiguous")) header += header;
        return """
                <div id='header' style='display:%s'>%s</div>
                <div id='product-1'><h4>Blue Top</h4><span class='cart_price'>Rs. 500</span>
                <span class='cart_quantity'><button>2</button></span><span class='cart_total'>Rs. 1000</span></div>
                <a class='check_out' href='#checkout' onclick='window.checkouts=(window.checkouts||0)+1'>Checkout</a>
                <script>setTimeout(()=>document.getElementById('header').style.display='block',150)</script>
                """.formatted(mode.equals("delayed") ? "none" : "block", header);
    }
}
