package atf.autoex.tests.support.invoice;

import atf.autoex.page.CheckoutPage;
import atf.autoex.runtime.invoice.AutoExInvoiceDownload;
import atf.autoex.ui.AutoExCheckoutPagePort;
import atf.autoex.business.invoice.AutoExInvoiceSnapshot;
import atf.autoex.locator.AutoExSelectors;
import atf.ui.config.*;
import atf.ui.playwright.*;
import com.microsoft.playwright.*;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Controlled documents exercise the real page port and browser download with no external purchase. */
public final class AutoExInvoiceDownloadFixture implements AutoCloseable {
    private final URI origin = URI.create("http://invoice-fixture.test");
    private final PlaywrightUiSession browser;
    private final PlaywrightActionSequence actions;
    private final CheckoutPage checkout;
    private final AtomicInteger clicks = new AtomicInteger(), gets = new AtomicInteger(), posts = new AtomicInteger();

    public AutoExInvoiceDownloadFixture(String mode) {
        browser = PlaywrightUiSession.open(new UiBrowserConfiguration(origin, "chromium", true,
                Duration.ofMillis(700), Duration.ofSeconds(3), 1000, 800, 100, true));
        actions = browser.newActionSequence();
        var page = browser.page();
        page.exposeFunction("activated", args -> clicks.incrementAndGet());
        page.route("**/*", route -> {
            String path = URI.create(route.request().url()).getPath();
            if (path.startsWith("/download_invoice/")) {
                int attempt = route.request().method().equals("GET") ? gets.incrementAndGet() : posts.incrementAndGet();
                boolean busy = mode.equals("queue-once") && attempt == 1 || mode.equals("always-busy")
                        || mode.equals("post") || mode.equals("backoff-budget");
                if (mode.equals("redirect")) { route.fulfill(new Route.FulfillOptions().setStatus(302)
                        .setHeaders(Map.of("location", "http://foreign.test/busy"))); return; }
                if (busy) { route.fulfill(new Route.FulfillOptions().setContentType("text/html")
                        .setBody("<h1>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h1>")); return; }
                if (mode.equals("no-event")) { route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody("<h1>File unavailable</h1>")); return; }
                route.fulfill(new Route.FulfillOptions().setContentType("text/plain")
                        .setHeaders(Map.of("Content-Disposition", "attachment; filename=invoice.txt", "Cache-Control", "no-store"))
                        .setBody("Invoice for owned customer; amount=1000")); return;
            }
            String body = path.equals("/busy") ? "<h1>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h1>"
                    : mode.equals("post") ? "<form action='/download_invoice/1000' method='post'>"
                    + "<a href='/download_invoice/1000' onclick='activated();event.preventDefault();this.parentElement.submit()'>Invoice</a></form>"
                    : "<a href='/download_invoice/1000' onclick='activated()'>Invoice</a>";
            route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(body));
        });
        page.navigate(origin + "/payment_done");
        var policy = new UiRetryPolicy(2, mode.equals("backoff-budget") ? Duration.ofSeconds(1) : Duration.ZERO,
                Set.of(UiRetryOperation.NAVIGATE), "fixture queue full");
        var download = new AutoExInvoiceDownload(page, actions, origin, policy);
        var port = (AutoExCheckoutPagePort) Proxy.newProxyInstance(AutoExCheckoutPagePort.class.getClassLoader(),
                new Class<?>[]{AutoExCheckoutPagePort.class}, (instance, method, args) -> switch (method.getName()) {
                    case "download" -> download.download(new AtfLocator(page.locator("a"), (String) args[0]), (String) args[1]);
                    default -> throw new AssertionError("Unexpected checkout capability: " + method.getName());
                });
        checkout = new CheckoutPage(port);
    }
    public AutoExInvoiceSnapshot invoice() { return checkout.downloadInvoice(); }
    public int clicks() { return clicks.get(); }
    public int gets() { return gets.get(); }
    public int posts() { return posts.get(); }
    public java.util.List<atf.core.ui.AtfUiActionRecord> records() { return actions.snapshot(); }
    @Override public void close() { browser.close(); }
}
