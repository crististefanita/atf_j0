package atf.autoex.tests.support.business;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.ui.AutoExAccountPagePort;
import com.microsoft.playwright.Page;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Test-only observed DOM corruption; real journeys, checkpoints and owned-account cleanup are delegated unchanged. */
public final class AutoExBusinessFault {
    public enum Point { ACCOUNT, CART, PURCHASE, RECALCULATION, REMOVAL, AFTER_SETUP }
    public static final String MARKER = "atf-business-fault";
    public static final String AFTER_SETUP_MESSAGE = "Controlled failure after account setup PASS";
    private static final java.util.concurrent.atomic.AtomicInteger CAPTURE_COLORS = new java.util.concurrent.atomic.AtomicInteger(0xC00000);
    private final int captureColor = CAPTURE_COLORS.incrementAndGet();
    private final Page page;
    private final Point point;
    private String selector;
    private boolean injected;
    private boolean restored;
    private int cleanupCalls;
    private int cartReads;

    public AutoExBusinessFault(Page page, Point point) { this.page = page; this.point = point; }

    public AutoExPurchaseSession session(AutoExE2eRun run) {
        return (AutoExPurchaseSession) Proxy.newProxyInstance(AutoExPurchaseSession.class.getClassLoader(),
                new Class<?>[]{AutoExPurchaseSession.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("text") || method.getName().equals("textFirst")) {
                        beforeRead((String) arguments[0]);
                    }
                    if (method.getName().equals("onFailureCleanup")) {
                        var owned = (AutoExAccountPagePort.AccountCleanup) arguments[0];
                        run.onFailureCleanup(() -> { cleanupCalls++; restore(); owned.run(); });
                        return null;
                    }
                    Object result;
                    try { result = method.invoke(run, arguments); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                    if (method.getName().equals("checkpoint")) afterCheckpoint((atf.autoex.support.trace.AutoExTraceStep) arguments[0]);
                    return result;
                });
    }

    /** Called only after the real checkpoint returned: the original PASS remains an observed predecessor. */
    public void afterCheckpoint(atf.autoex.support.trace.AutoExTraceStep step) {
        if (restored || point != Point.AFTER_SETUP || injected || step != atf.autoex.support.journey.AutoExPurchaseTraceStep.ACCOUNT_CREATED) return;
        mark();
        throw new IllegalStateException(AFTER_SETUP_MESSAGE);
    }

    public void beforeRead(String candidate) {
        if (restored || injected || !matches(candidate)) return;
        if (point == Point.CART && !page.locator(AutoExSelectors.LOGGED_IN_NAME).isVisible()) return;
        if (point == Point.RECALCULATION && ++cartReads < 2) return;
        selector = candidate;
        page.locator(candidate).evaluate("""
                (element, input) => {
                    element.setAttribute('data-atf-fault-original', element.textContent);
                    element.setAttribute('data-atf-fault-node', input.marker);
                    element.textContent = input.value;
                }
                """, Map.of("marker", MARKER, "point", point.name(), "value", observedValue()));
        mark();
    }

    private void mark() {
        page.evaluate("""
                input => {
                    const badge = document.createElement('div'); badge.id = input.marker;
                    badge.textContent = 'CONTROLLED BUSINESS FAILURE: ' + input.point;
                    badge.style.cssText = 'position:fixed;left:16px;top:100px;width:300px;height:44px;'
                        + 'background:#' + input.color + ';color:#fff;font:12px sans-serif;z-index:2147483647;';
                    document.body.appendChild(badge);
                }
                """, Map.of("marker", MARKER, "point", point.name(), "color", Integer.toHexString(captureColor)));
        injected = true;
    }

    private boolean matches(String candidate) {
        return switch (point) {
            case ACCOUNT -> candidate.equals(AutoExSelectors.LOGGED_IN_NAME);
            case CART, RECALCULATION -> candidate.endsWith(AutoExSelectors.CART_QUANTITY);
            case PURCHASE -> candidate.equals(AutoExSelectors.ORDER_SUCCESS);
            case REMOVAL -> candidate.equals(AutoExSelectors.EMPTY_CART);
            case AFTER_SETUP -> false;
        };
    }

    private String observedValue() { return point == Point.CART || point == Point.RECALCULATION ? "99" : "ATF controlled " + point + " mismatch"; }

    /** Ends the fault window even when unreached, then restores its marked node before the original cleanup callback. */
    public void restore() {
        restored = true;
        if (!injected || page.isClosed()) return;
        page.evaluate("""
                marker => {
                    for (const element of document.querySelectorAll('[data-atf-fault-node]')) {
                        if (element.getAttribute('data-atf-fault-node') !== marker) continue;
                        element.textContent = element.getAttribute('data-atf-fault-original');
                        element.removeAttribute('data-atf-fault-node'); element.removeAttribute('data-atf-fault-original');
                    }
                    document.getElementById(marker)?.remove();
                }
                """, MARKER);
    }
    public boolean injected() { return injected; }
    public String selector() { return selector; }
    public int cleanupCalls() { return cleanupCalls; }
    public int captureColor() { return captureColor; }

    /** Browser ownership for the offline component: the executable test consumes observations, not a native page. */
    public static final class LocalFixture implements AutoCloseable {
        private final com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create();
        private final com.microsoft.playwright.Browser browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
        private final Page page = browser.newPage();
        public LocalFixture() {
            page.setContent("""
                    <header id="header"><a><i class="fa-user"></i><b>Owned Account</b></a></header>
                    <table><tr><td class="cart_quantity"><button>2</button></td></tr></table>
                    <h2 data-qa="order-placed">Order Placed!</h2>
                    <p id="empty_cart">Cart is empty!</p>
                    """);
        }
        public AutoExBusinessFault fault(Point point) { return new AutoExBusinessFault(page, point); }
        public String text(String selector) { return page.locator(selector).textContent(); }
        public String url() { return page.url(); }
        public int markers() { return page.locator("#" + MARKER).count(); }
        public int markedNodes() { return page.locator("[data-atf-fault-node]").count(); }
        public atf.autoex.support.account.AutoExAccountDeletionAttempt.Traffic deletionTrafficAfter(String url) {
            page.route("**/*", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(200).setBody("controlled response").setHeaders(Map.of("Access-Control-Allow-Origin", "*"))));
            try (var attempt = new atf.autoex.support.account.AutoExAccountDeletionAttempt(
                    page, java.net.URI.create("http://store.local/delete_account"))) {
                page.evaluate("url => fetch(url).then(response => response.text())", url);
                return attempt.traffic();
            }
        }
        @Override public void close() { try { browser.close(); } finally { playwright.close(); } }
    }
}
