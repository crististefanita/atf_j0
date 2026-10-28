package atf.ui.recovery.browser;

import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Local application performs its own bounded GET retry; ATF receives no dispatch callback. */
final class RecoveryBrowserFixture implements AutoCloseable {
    final AtomicInteger reads = new AtomicInteger(), writes = new AtomicInteger();
    final CountDownLatch release = new CountDownLatch(1);
    final HttpServer server;
    final ExecutorService executor = Executors.newCachedThreadPool(task -> { var thread = new Thread(task); thread.setDaemon(true); return thread; });
    final Playwright playwright = Playwright.create();
    final Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    final Page page = browser.newPage();
    final String mode;
    RecoveryBrowserFixture(String mode) throws Exception {
        this.mode = mode;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int status = 200;
            String body;
            if (path.equals("/catalog")) {
                int number = reads.incrementAndGet();
                try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                status = (mode.equals("transient") || mode.equals("query-change")) && number == 1 ? 503 : mode.equals("exhaust") ? 503 : 200;
                body = "{\"ready\":true}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            } else if (path.equals("/add_to_cart")) { writes.incrementAndGet(); body = "{}"; }
            else if (path.equals("/noise")) { status = 503; body = "{}"; }
            else if (path.equals("/frame")) { body = "<script>function read(){return fetch('/catalog').then(r=>r.json())}</script>"; }
            else {
                body = """
                        <html><body><button id='read'>Read catalog</button><button id='write'>Write once</button>
                        <div id='ready' hidden>ready</div><iframe src='/frame'></iframe>
                        <script>
                        window.clicks=0;
                        const mode=new URLSearchParams(location.search).get('mode');
                        async function load(second=false){
                          let target=mode==='noise'?'/noise':mode==='query-change'&&second?'/catalog?different=2':'/catalog';
                          let response=await fetch(target);
                          if(response.ok){await response.json();document.getElementById('ready').hidden=false;}
                          else if(!second&&(mode==='transient'||mode==='query-change'))setTimeout(()=>load(true),40);
                        }
                        document.getElementById('read').onclick=()=>{window.clicks++;if(mode==='frame')document.querySelector('iframe').contentWindow.read();else load();};
                        document.getElementById('write').onclick=()=>{window.clicks++;fetch('/add_to_cart');};
                        </script></body></html>
                        """;
                exchange.getResponseHeaders().set("Content-Type", "text/html");
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start(); page.navigate(uri("/?mode=" + mode).toString());
    }
    URI uri(String route) { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + route); }
    boolean ready() { return page.locator("#ready").isVisible(); }
    int clicks() { return ((Number) page.evaluate("window.clicks")).intValue(); }
    @Override public void close() {
        release.countDown();
        try (playwright) { browser.close(); } finally { server.stop(0); executor.shutdownNow(); }
    }
}
