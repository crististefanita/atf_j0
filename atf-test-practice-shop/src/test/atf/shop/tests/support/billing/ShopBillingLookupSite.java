package atf.shop.tests.support.billing;

import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.*;
import java.net.URI;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Controlled application debounce, HTTP completion and form application, with a native-fill race injection. */
public final class ShopBillingLookupSite implements AutoCloseable {
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger completed = new AtomicInteger();
    private final java.util.Map<String, AtomicInteger> inputEvents = new java.util.concurrent.ConcurrentHashMap<>();
    private final Page actual;
    private final Page port;
    private final URI origin;

    public ShopBillingLookupSite(Page page, String mode) throws Exception {
        actual = page;
        page.onConsoleMessage(message -> { if(message.text().startsWith("billing-input:"))
            inputEvents.computeIfAbsent(message.text().substring(14),ignored -> new AtomicInteger()).incrementAndGet(); });
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        server.setExecutor(executor);
        String template;
        try (var source = getClass().getResourceAsStream("/billing/postcode-lookup.html")) {
            template = new String(java.util.Objects.requireNonNull(source).readAllBytes(), StandardCharsets.UTF_8).replace("__MODE__", mode);
        }
        server.createContext("/", exchange -> {
            int status = 200; String body;
            if (exchange.getRequestURI().getPath().equals("/postcode-lookup")) {
                int sequence = requests.incrementAndGet();
                boolean old = java.util.Objects.toString(exchange.getRequestURI().getQuery(), "").contains("postcode=old");
                long delay = old ? 650 : mode.equals("pending") || mode.equals("document") ? 450
                        : mode.equals("duplicate") && sequence == 2 ? 180 : 0;
                try { Thread.sleep(delay); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                if (mode.equals("error-status")) status = 500;
                body = old ? "{\"street\":\"Old street\",\"city\":\"Old city\",\"state\":\"Old state\"}"
                        : "{\"street\":\"Wayne Lock\",\"city\":\"Cartwrightmouth\",\"state\":\"Alaska\"}";
                if (mode.equals("schema")) body = "{\"street\":\"\",\"city\":\"Cartwrightmouth\",\"state\":\"Alaska\"}";
                completed.incrementAndGet();
            } else body = template;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", exchange.getRequestURI().getPath().equals("/postcode-lookup")
                        ? "application/json" : "text/html; charset=utf-8");
                exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes);
            } catch (java.io.IOException closedAfterDeadline) { /* a negative case can close before the controlled response */ }
        });
        server.start();
        port = (Page) Proxy.newProxyInstance(Page.class.getClassLoader(), new Class<?>[]{Page.class}, (proxy, method, args) -> {
            Object result = invoke(method, actual, args);
            if (!mode.equals("race") || !method.getName().equals("locator") || !args[0].toString().equals("input[data-test='state']")) return result;
            Locator target = (Locator) result;
            return Proxy.newProxyInstance(Locator.class.getClassLoader(), new Class<?>[]{Locator.class}, (p, operation, values) -> {
                if (operation.getName().equals("fill")) actual.waitForFunction("window.pendingApply !== null");
                return invoke(operation, target, values);
            });
        });
    }
    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try { return method.invoke(target, arguments); } catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    public void open() { actual.navigate(origin + "/checkout"); }
    public Page page() { return port; }
    public URI endpoint() { return origin.resolve("/postcode-lookup"); }
    public int requests() { return requests.get(); }
    public int completed() { return completed.get(); }
    public String state() { return actual.locator("input[data-test='state']").inputValue(); }
    public int inputs(String field) { return inputEvents.getOrDefault(field, new AtomicInteger()).get(); }
    public int applied() { return ((Number) actual.evaluate("window.applyCount")).intValue(); }
    public List<String> address() { return List.of(actual.locator("#street").inputValue(), actual.locator("#city").inputValue(), state()); }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
