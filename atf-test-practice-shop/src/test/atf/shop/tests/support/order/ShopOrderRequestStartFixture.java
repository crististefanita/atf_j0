package atf.shop.tests.support.order;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** A real old POST stays in flight across Confirm; only the fixture controls its release. */
public final class ShopOrderRequestStartFixture implements AutoCloseable {
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService workers = Executors.newCachedThreadPool();
    private final CountDownLatch releaseOld = new CountDownLatch(1);
    private final AtomicInteger oldRequests = new AtomicInteger();
    private final AtomicInteger currentRequests = new AtomicInteger();

    public ShopOrderRequestStartFixture(String route, int currentStatus) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(workers);
        server.createContext("/checkout", exchange -> respond(exchange, 200, "text/html", """
                <!doctype html><html><body><button onclick="submitOrder()">Confirm</button>
                <div>Thanks for your order! Your invoice number is INV-12.</div>
                <script>
                window.clicks=0; window.oldDone=false;
                const route='%s';
                window.startOld=()=>fetch(route,{method:'POST',body:'old'})
                    .then(r=>r.text()).then(()=>window.oldDone=true);
                async function submitOrder(){
                    window.clicks++;
                    if (%d===0) { await window.releaseOld(); return; }
                    await fetch(route,{method:'POST',body:'current'}).then(r=>r.text());
                    await window.releaseOld();
                }
                </script></body></html>
                """.formatted(route, currentStatus)));
        server.createContext(route, exchange -> {
            boolean old = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).equals("old");
            if (old) {
                oldRequests.incrementAndGet();
                try { releaseOld.await(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); exchange.close(); return; }
            } else currentRequests.incrementAndGet();
            String body = old || currentStatus < 400
                    ? "{\"id\":\"" + (old ? "old-order" : "current-order") + "\",\"invoice_number\":\"INV-12\",\"total\":24.02}"
                    : "{\"message\":\"current order failed\",\"password\":\"raw-order-secret\"}";
            respond(exchange, old ? 201 : currentStatus, "application/json", body);
        });
        server.start();
    }

    public String origin() { return "http://api.practicesoftwaretesting.com:" + server.getAddress().getPort(); }
    public int oldRequests() { return oldRequests.get(); }
    public int currentRequests() { return currentRequests.get(); }
    public void releaseOld() { releaseOld.countDown(); }

    private static void respond(HttpExchange exchange, int status, String contentType, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override public void close() {
        releaseOld();
        server.stop(0);
        workers.shutdownNow();
    }
}
