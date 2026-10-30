package atf.shop.tests.support.cart;

import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopProductPage;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Local mutation/response fixture; executable consumer tests see typed counters and the real page adapter. */
public final class ShopCartAcknowledgementFixture implements AutoCloseable {
    private final URI origin = URI.create("http://shop-cart.test");
    private final PlaywrightUiSession browser;
    private final PlaywrightActionSequence actions;
    private final AtomicInteger clicks = new AtomicInteger(), starts = new AtomicInteger();
    private Route old;
    private int persisted;

    public ShopCartAcknowledgementFixture(String mode) {
        browser = PlaywrightUiSession.open(new UiBrowserConfiguration(origin, "chromium", true,
                Duration.ofMillis(600), Duration.ofSeconds(3), 1000, 800, 100, true));
        actions = browser.newActionSequence(); var page = browser.page();
        page.exposeFunction("activated", args -> clicks.incrementAndGet());
        page.route("**/*", route -> {
            var request = route.request(); String path = URI.create(request.url()).getPath();
            if (path.equals("/release-old")) { old.fulfill(json(200)); route.fulfill(new Route.FulfillOptions().setBody("released")); return; }
            if (path.startsWith("/carts/")) {
                if (request.method().equals("OPTIONS")) { route.fulfill(json(204)); return; }
                starts.incrementAndGet();
                if (mode.equals("old-response") && old == null) { old = route; return; }
                if (mode.equals("transport")) { route.abort("failed"); return; }
                try {
                    persisted = mode.equals("rejected") ? 0 : new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(request.postData()).path("quantity").intValue();
                } catch (com.fasterxml.jackson.core.JsonProcessingException malformed) {
                    throw new AssertionError("Fixture received invalid cart JSON", malformed);
                }
                route.fulfill(json(mode.equals("rejected") ? 500 : 200)); return;
            }
            String endpoint = mode.equals("foreign") ? "http://foreign-cart.test/carts/cart-id" : "/carts/cart-id";
            String product = mode.equals("wrong-product") ? "other" : "pliers-id";
            int quantity = mode.equals("wrong-quantity") ? 1 : 2;
            String operation = mode.equals("absent") ? "" : mode.equals("old-response") ? "fetch('/release-old')"
                    : "fetch('" + endpoint + "',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({product_id:'"
                    + product + "',quantity:" + quantity + "})}).then(()=>{"
                    + (mode.equals("no-toast") ? "" : "document.querySelector('output').textContent='Product added to shopping cart';")
                    + (mode.equals("short-toast") ? "setTimeout(()=>document.querySelector('output').textContent='',0);" : "") + "})";
            route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody("""
                    <input data-test='quantity' type='number' value='1'>
                    <button data-test='add-to-cart' onclick="activated();%s">Add</button><output></output>
                    """.formatted(operation)));
        });
        page.navigate(origin + "/product/pliers-id");
        if (mode.equals("old-response")) page.waitForRequest(r -> r.method().equals("POST"), () -> page.evaluate("""
                void fetch('/carts/cart-id',{method:'POST',headers:{'Content-Type':'application/json'},
                  body:JSON.stringify({product_id:'pliers-id',quantity:2})})
                """));
    }
    private static Route.FulfillOptions json(int status) {
        return new Route.FulfillOptions().setStatus(status).setContentType("application/json")
                .setHeaders(Map.of("Access-Control-Allow-Origin", "*", "Access-Control-Allow-Methods", "POST,OPTIONS", "Access-Control-Allow-Headers", "content-type"))
                .setBody("{\"result\":\"item added\"}");
    }
    public void add() { new ShopProductPage(browser.page(), new ShopLocators(), actions, origin).addConfiguredQuantity(2); }
    public int clicks() { return clicks.get(); }
    public int starts() { return starts.get(); }
    public int persisted() { return persisted; }
    public java.util.List<atf.core.ui.AtfUiActionRecord> records() { return actions.snapshot(); }
    @Override public void close() { browser.close(); }
}
