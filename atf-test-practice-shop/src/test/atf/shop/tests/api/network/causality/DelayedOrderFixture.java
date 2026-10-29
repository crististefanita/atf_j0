package atf.shop.tests.api.network.causality;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** An old request begins before Confirm and can finish after the optional new request. */
final class DelayedOrderFixture implements AutoCloseable {
    final CountDownLatch oldReceived = new CountDownLatch(1), release = new CountDownLatch(1);
    final AtomicInteger dispatches = new AtomicInteger();
    final HttpServer server;
    final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    DelayedOrderFixture(String route, int currentStatus) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.setExecutor(workers);
        server.createContext("/page", exchange -> {
            String action = currentStatus == 0 ? "" : "window.currentOrder=fetch('" + route + "',{method:'POST',body:'{}'}).then(r=>r.text());";
            send(exchange, 200, "text/html", "<button onclick=\"window.confirms++;" + action
                    + "\">Confirm</button><script>window.confirms=0</script>");
        });
        server.createContext(route, exchange -> {
            int number = dispatches.incrementAndGet();
            if (number == 1) {
                oldReceived.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Fixture response was not released"); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            }
            send(exchange, number == 1 ? 201 : currentStatus, "application/json", "{\"id\":\""
                    + (number == 1 ? "old-order" : "current-order") + "\",\"invoice_number\":\"INV-12\",\"total\":24.02}");
        });
        server.start();
    }
    String origin() { return "http://api.practicesoftwaretesting.com:" + server.getAddress().getPort(); }
    private static void send(com.sun.net.httpserver.HttpExchange exchange, int status, String type, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    }
    public void close() { release.countDown(); server.stop(0); workers.close(); }
}
