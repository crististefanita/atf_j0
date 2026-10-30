package atf.ui.tests.support.geometry;

import com.sun.net.httpserver.HttpServer;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.time.Duration;
import java.util.Objects;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Real loopback origins and nested frames; each document owns its event counters and controls. */
public final class PointerGeometryFixture implements AutoCloseable {
    public static final Duration NAVIGATION_TIMEOUT = Duration.ofSeconds(5);
    private final HttpServer main, other;
    private final Duration initialResponseDelay, initializationDelay;
    private final ExecutorService workers=Executors.newCachedThreadPool();
    public PointerGeometryFixture() throws Exception { this(Duration.ZERO, Duration.ZERO); }
    public PointerGeometryFixture(Duration initializationDelay) throws Exception { this(Duration.ZERO, initializationDelay); }
    public PointerGeometryFixture(Duration initialResponseDelay, Duration initializationDelay) throws Exception {
        this.initialResponseDelay = checked(initialResponseDelay);
        this.initializationDelay = checked(initializationDelay);
        main=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        other=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        main.createContext("/",exchange->{
            try(exchange){
                String route=exchange.getRequestURI().getPath();
                if (route.equals("/")) delayInitialResponse();
                String html=route.equals("/frame")?controls():document();
                byte[] body=html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");
                exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);
            }
        });
        other.createContext("/",exchange->{
            try(exchange){
                String html=exchange.getRequestURI().getPath().equals("/nested")
                        ?"<iframe id='inner' src='"+home().resolve("frame")+"'></iframe><script>document.body.dataset.geometryReady='true'</script>":controls();
                byte[] body=html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");
                exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);
            }
        });
        main.setExecutor(workers);other.setExecutor(workers);main.start();other.start();
    }
    /** Fixture preparation owns the existing navigation budget; native input keeps its separate 850ms cap. */
    public PreparedPage openReady(Page page) {
        Objects.requireNonNull(page, "page");
        long started = System.nanoTime();
        try {
            page.navigate(home().toString(), new Page.NavigateOptions().setTimeout(remaining(started)));
            var documents = page.frames();
            if (documents.size() != 5) throw new IllegalStateException("Geometry fixture requires its five actual documents");
            for (var frame : documents) frame.locator("body[data-geometry-ready='true']")
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(remaining(started)));
            // LOAD and a first capture do not prove that the root's frame layout has settled.
            page.waitForFunction("""
                () => {
                    const current = [...document.querySelectorAll('#frames iframe')].map(frame => {
                        const box = frame.getBoundingClientRect();
                        return [box.x, box.y, box.width, box.height];
                    });
                    const previous = window.geometryLayout;
                    const stable = previous && current.length === 3 && current.every((box, index) =>
                        box.every((value, axis) => value === previous.boxes[index][axis]));
                    window.geometryLayout = {boxes: current, samples: stable ? previous.samples + 1 : 1};
                    return stable && window.geometryLayout.samples >= 3;
                }
                """, null, new Page.WaitForFunctionOptions().setTimeout(remaining(started)));
            // A native viewport capture fences first paint without activating, focusing or pre-scrolling any target.
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setTimeout(remaining(started)));
            remaining(started);
            return new PreparedPage(screenshot, Duration.ofNanos(System.nanoTime()-started).toMillis(), documents.size());
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Geometry fixture preparation failed within its existing 5000ms navigation budget", failure);
        }
    }
    public record PreparedPage(byte[] screenshot, long elapsedMillis, int documents) {
        public PreparedPage { screenshot = screenshot.clone(); }
        @Override public byte[] screenshot() { return screenshot.clone(); }
    }
    private static double remaining(long started) {
        long nanos = NAVIGATION_TIMEOUT.toNanos() - (System.nanoTime() - started);
        if (nanos <= 0) throw new IllegalStateException("Geometry fixture navigation budget exhausted");
        return Math.max(1d, nanos / 1_000_000d);
    }
    private static Duration checked(Duration value) {
        Objects.requireNonNull(value, "delay");
        if (value.isNegative() || value.compareTo(Duration.ofMillis(Integer.MAX_VALUE))>0)
            throw new IllegalArgumentException("Fixture delay must fit a non-negative browser timer");
        return value;
    }
    private void delayInitialResponse() throws java.io.IOException {
        try { Thread.sleep(initialResponseDelay.toMillis()); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new java.io.IOException("Fixture response interrupted", interrupted); }
    }
    public URI home(){return URI.create("http://127.0.0.1:"+main.getAddress().getPort()+"/");}
    private URI other(){return URI.create("http://127.0.0.1:"+other.getAddress().getPort()+"/");}
    private String document(){
        return """
            <!doctype html><html style='--fixture-owner:keep'><style>
            body{margin:0;background:white;font:16px/18px Arial}#marker{position:absolute;left:100px;top:100px;width:200px;height:100px;background:rgb(255,0,0)}
            iframe{display:block;width:600px;height:220px;margin-left:80px;border:4px solid black;padding:6px}
            #frames{padding-top:260px}#cover{display:none;position:fixed;inset:0;background:white;z-index:10}
            #tail{height:1700px}#fixed{position:fixed;right:0;top:0;width:30px;height:30px;background:blue}
            </style><body data-geometry-ready='true'><div id='marker'></div><div id='fixed'></div><a id='popup' href='/frame' target='_blank'>Popup</a><div id='frames'>
            <iframe id='same' src='/frame'></iframe>
            <iframe id='cross' src='%s'></iframe>
            <iframe id='nested' src='%s'></iframe></div><div id='tail'></div><div id='cover'></div>
            <script>window.childActivations=0;addEventListener('message',e=>{if(e.data==='geometry-click')window.childActivations++;});</script>
            </body></html>
            """.formatted(other().resolve("frame"),other().resolve("nested"));
    }
    private String controls(){
        return """
            <!doctype html><html><body style='margin:12px'>
            <button id='activate' style='margin:20px;width:120px;height:40px'>Activate</button>
            <input id='entry' style='position:absolute;left:220px;top:80px;width:140px;height:30px;box-sizing:border-box;border:0;padding:0;background:lime'>
            <button id='disabled' disabled>Disabled</button>
            <output id='clicks'>0</output><output id='trusted'>false</output><output id='keys'>0</output>
            <script>
            const button=document.getElementById('activate');
            button.onclick=e=>{document.getElementById('clicks').textContent=String(Number(document.getElementById('clicks').textContent)+1);
                document.getElementById('trusted').textContent=String(e.isTrusted);window.top.postMessage('geometry-click','*');};
            document.getElementById('entry').onkeydown=e=>{if(e.isTrusted)document.getElementById('keys').textContent=String(Number(document.getElementById('keys').textContent)+1);};
            const initialize=()=>{button.disabled=false;document.body.dataset.geometryReady='true'};
            if(%d===0)document.body.dataset.geometryReady='true';
            else {button.disabled=true;setTimeout(initialize,%d);}
            </script></body></html>
            """.formatted(initializationDelay.toMillis(),initializationDelay.toMillis());
    }
    @Override public void close(){try{main.stop(0);}finally{try{other.stop(0);}finally{workers.shutdownNow();}}}
}
