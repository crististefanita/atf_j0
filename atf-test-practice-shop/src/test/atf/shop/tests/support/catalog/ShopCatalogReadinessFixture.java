package atf.shop.tests.support.catalog;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Owned loopback server; all scripted failures and POST probes are isolated from the public shop. */
public final class ShopCatalogReadinessFixture implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        var thread = new Thread(r, "owned-catalog-fixture");thread.setDaemon(true);return thread;
    });
    public final AtomicInteger documents = new AtomicInteger();
    public final AtomicInteger requests = new AtomicInteger();
    public final List<String> methods = new CopyOnWriteArrayList<>();
    public final List<String> bodies = new CopyOnWriteArrayList<>();
    private final String mode;
    private final CountDownLatch readinessStarted = new CountDownLatch(1);

    public ShopCatalogReadinessFixture(String mode) throws Exception {
        this.mode = mode;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] document;
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/atf/shop/catalog-readiness.html"))) {
            document = input.readAllBytes();
        }
        server.createContext("/", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                byte[] body;
                int status = 200;
                if (path.equals("/")) {
                    documents.incrementAndGet();body = document;
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                } else if (path.endsWith("/products")) {
                    int attempt = requests.incrementAndGet();methods.add(exchange.getRequestMethod());
                    bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    if (Set.of("budget", "redirect", "unexpected").contains(mode) && attempt == 1) {
                        try {
                            if (!readinessStarted.await(8, TimeUnit.SECONDS)) throw new IllegalStateException("Readiness was not entered");
                            if (mode.equals("budget")) Thread.sleep(400);
                        } catch (InterruptedException stopped) {Thread.currentThread().interrupt();}
                    }
                    status = switch (mode) {
                        case "transient", "budget" -> attempt == 1 ? 503 : 200;
                        case "exhausted", "unrelated", "post" -> 503;
                        case "forbidden" -> 403;
                        case "retry-after" -> 429;
                        default -> 200;
                    };
                    if (mode.equals("retry-after")) exchange.getResponseHeaders().set("Retry-After", "2");
                    body = (mode.equals("empty") ? "[]" : "[{\"id\":1,\"name\":\"Owned Tool\"}]")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                } else {body = "Checkout is not a catalog".getBytes(StandardCharsets.UTF_8);}
                exchange.sendResponseHeaders(status, body.length);exchange.getResponseBody().write(body);
            }
        });
        server.setExecutor(workers);server.start();
    }

    public URI home(String method) { return URI.create(origin() + "/?mode=" + mode + "&method=" + method); }
    public URI endpoint() { return URI.create(origin() + "/products"); }
    public void readinessStarted() { readinessStarted.countDown(); }
    private String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    @Override public void close() { try {server.stop(0);} finally {workers.shutdownNow();} }
}
