package atf.autoex.tests.support.product;

import com.microsoft.playwright.Page;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual HTTP documents and independent activation/request counters; no public site traffic. */
public final class AutoExProductTransitionSite implements AutoCloseable {
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService executor = Executors.newCachedThreadPool();
    private final List<String> requests = java.util.Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger activations = new AtomicInteger();
    private final AtomicInteger closes = new AtomicInteger();
    private final AtomicInteger details = new AtomicInteger();
    private final URI origin;

    public AutoExProductTransitionSite(Page page, String mode) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        page.onConsoleMessage(message -> { if (message.text().equals("product-pointer:true")) activations.incrementAndGet();
            if (message.text().equals("product-close:false")) closes.incrementAndGet(); });
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int status = 200;
            String body;
            if (path.equals("/products")) {
                String href = mode.equals("unsafe-target") ? "/product_details/1?mutation=true" : "/product_details/1";
                String effect = (mode.equals("post-document") || mode.equals("post-525")) ? "event.preventDefault();document.querySelector('form').submit();" : "";
                if (mode.startsWith("vignette")) effect = "event.preventDefault();location.hash='google_vignette';"
                        + "setTimeout(()=>{let f=document.createElement('iframe');f.src='/vignette';document.body.append(f);},60);";
                body = "<div class='features_items'><div class='productinfo'><p>Blue Top</p></div></div>"
                        + "<a href='" + href + "' onclick=\"console.log('product-pointer:'+event.isTrusted);" + effect + "\">Details</a>"
                        + "<form method='post' action='/product_details/1'></form>";
            } else if (path.equals("/vignette")) {
                String close = "<button id='dismiss-button' onclick=\"console.log('product-close:'+event.isTrusted);"
                        + (mode.equals("vignette-stuck") ? "" : "parent.location.href='/product_details/1';") + "\">Close</button>";
                body = mode.equals("vignette-missing") ? "<p>No Close</p>"
                        : mode.equals("vignette-ambiguous") ? close + close
                        : mode.equals("vignette-late") ? "<script>setTimeout(()=>document.body.innerHTML="
                                + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(close) + ",120)</script>"
                        : close;
            } else if (path.matches("/product_details/[0-9]+")) {
                int attempt = details.incrementAndGet(); requests.add(exchange.getRequestMethod() + " " + path);
                boolean transientFailure = mode.equals("repeat-520") || mode.equals("520-once") && attempt == 1 || mode.equals("vignette-520") && attempt == 1
                        || mode.equals("repeat-525") || mode.equals("post-525") || mode.equals("525-once") && attempt == 1;
                if (transientFailure) { status = mode.contains("525") ? 525 : 520; body = "<h1>Unavailable server document</h1><p>Error code " + status + "</p>"; }
                else if (mode.equals("queue-once") && attempt == 1) body = "<p>This website is under heavy load (queue full)</p>";
                else if (mode.equals("queue-late") && attempt == 1) body = "<script>setTimeout(()=>document.body.innerHTML="
                        + "\"<p>This  website is under heavy load (queue full)</p>\",120)</script>";
                else if (mode.equals("missing")) body = "<h1>Incomplete response</h1>";
                else if (mode.equals("late")) body = "<script>setTimeout(()=>document.body.innerHTML=\"<div class='product-information'><h2>Blue Top</h2></div>\",120)</script>";
                else body = "<div class='product-information'><h2>" + (mode.equals("wrong-product") ? "Other product" : "Blue Top") + "</h2></div>";
                if (mode.equals("ambiguous-product")) body += "<div class='product-information'><h2>Blue Top</h2></div>";
                if (mode.equals("forbidden")) status = 403;
            } else {status = 404; body = "Not found";}
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes);
            } catch (java.io.IOException closedAfterDeadline) { /* the bounded browser can close before a deliberately delayed reply */ }
        });
        server.start();
    }
    /** Fixture rendering is preparation, bounded by the existing 3-second navigation allowance; no pointer preflight. */
    public void openReady(Page page) {
        long started = System.nanoTime();
        page.navigate(origin + "/products");
        var link = page.locator("a[href^='/product_details/']");
        link.waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                .setTimeout(preparationRemaining(started)));
        page.screenshot(new Page.ScreenshotOptions().setTimeout(preparationRemaining(started)));
        if (activations() != 0 || !requests().isEmpty()) throw new IllegalStateException("Preparation dispatched product input");
    }
    private static double preparationRemaining(long started) {
        long remaining = java.time.Duration.ofSeconds(3).toNanos() - (System.nanoTime() - started);
        if (remaining <= 0) throw new IllegalStateException("Product fixture preparation exceeded its original navigation budget");
        return Math.max(1d, remaining / 1_000_000d);
    }
    public URI origin() { return origin; }
    public int closes() { return closes.get(); }
    public int activations() { return activations.get(); }
    public List<String> requests() { synchronized (requests) { return List.copyOf(requests); } }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
