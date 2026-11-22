package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import com.sun.net.httpserver.HttpServer;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Current-document pixels and trusted events certify setup; native actionability remains independent. */
@AtfComponentTest
class PlaywrightPagePreparationComponentTest {
    private static final String CONTROLS = """
        <style>body{margin:0}#marker{position:fixed;left:0;top:0;width:40px;height:40px;background:red}
        #activate{position:absolute;left:100px;top:80px;width:120px;height:40px}#entry{position:absolute;left:100px;top:140px}</style>
        <div id='marker'></div><button id='activate'>Activate</button><input id='entry'>
        <script>window.clicks=0;window.inputs=0;window.trustedClick=false;window.trustedInput=false;window.ready=false;
        activate.onclick=e=>{clicks++;trustedClick=e.isTrusted;setTimeout(()=>ready=true,80)};
        entry.oninput=e=>{inputs++;trustedInput=e.isTrusted};</script>
        """;

    @ParameterizedTest @ValueSource(ints = {70, 100})
    void nativePaintPrecedesOneMeasuredClickAndFillAfterColdInitialization(int zoom) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String html = "<!doctype html><body>" + CONTROLS + """
                <script>window.startupFinished=false;addEventListener('load',()=>setTimeout(()=>{
                  const end=performance.now()+1200;while(performance.now()<end){}
                  startupFinished=true;marker.style.background='lime';
                },0));</script>
                """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        URI origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (var session = open(origin, zoom)) {
            var prepared = new PlaywrightPagePreparation(session.page(), Duration.ofSeconds(5)).navigate(origin + "/start");
            var image = ImageIO.read(new ByteArrayInputStream(prepared.viewportPng()));
            assertEquals(1000, image.getWidth()); assertEquals(800, image.getHeight());
            assertEquals(0x00ff00, image.getRGB(5, 5) & 0xffffff, "The receipt must contain the initialized document's real lime pixels");
            var actions = session.newActionSequence();
            assertEquals(0, actions.snapshot().size(), "Preparation never records a synthetic successful action");
            actions.clickAndAwait(new AtfLocator(session.page().locator("#activate"), "#activate"), "one native click and ready", budget ->
                    session.page().waitForFunction("window.ready", null,
                            new com.microsoft.playwright.Page.WaitForFunctionOptions().setTimeout(budget.remainingMilliseconds())).dispose());
            actions.fill(new AtfLocator(session.page().locator("#entry"), "#entry"), "value", "one native input");
            assertEquals(true, session.page().evaluate("startupFinished"));
            assertEquals(1, session.page().evaluate("clicks")); assertEquals(1, session.page().evaluate("inputs"));
            assertEquals(true, session.page().evaluate("trustedClick && trustedInput"));
            assertEquals(2, actions.snapshot().size());
            assertTrue(actions.snapshot().stream().allMatch(record -> record.status().name().equals("PASS")));
            assertEquals("CSS1Compat", session.page().evaluate("document.compatMode"));
            assertEquals("", session.page().evaluate("document.documentElement.style.zoom"));
        } finally { server.stop(0); }
    }

    @ParameterizedTest @ValueSource(strings = {"disabled", "covered", "moving"})
    void aPaintedDocumentNeverOverridesNativeTargetActionability(String mode) throws Exception {
        try (var session = open(URI.create("about:blank"), 100)) {
            String obstruction = switch (mode) {
                case "disabled" -> "<script>activate.disabled=true</script>";
                case "covered" -> "<div style='position:absolute;left:95px;top:75px;width:130px;height:50px;background:black'></div>";
                default -> "<style>@keyframes move{from{transform:translateX(0)}to{transform:translateX(100px)}}#activate{animation:move .3s linear infinite alternate}</style>";
            };
            var prepared = new PlaywrightPagePreparation(session.page(), Duration.ofSeconds(5)).setContent(CONTROLS + obstruction);
            assertNotNull(ImageIO.read(new ByteArrayInputStream(prepared.viewportPng())));
            var actions = session.newActionSequence();
            var confirmations = new java.util.concurrent.atomic.AtomicInteger();
            assertThrows(PlaywrightException.class, () -> actions.clickAndAwait(
                    new AtfLocator(session.page().locator("#activate"), "#activate"), "must remain refused", budget -> confirmations.incrementAndGet()));
            assertEquals(0, confirmations.get()); assertEquals(0, session.page().evaluate("clicks"));
            assertEquals(1, actions.snapshot().size()); assertEquals("FAIL", actions.snapshot().getFirst().status().name());
        }
    }

    @Test void eachReplacementHasFreshPixelsAndPreservesTheOriginalDocumentMode() throws Exception {
        try (var session = open(URI.create("about:blank"), 100)) {
            var preparation = new PlaywrightPagePreparation(session.page(), Duration.ofSeconds(5));
            var first = preparation.setContent("<body style='margin:0;background:red'><input readonly style='position:absolute;left:100px;top:80px'></body>");
            var second = preparation.setContent("<body style='margin:0;background:blue'><input readonly style='position:absolute;left:100px;top:80px'></body>");
            assertEquals(0xff0000, ImageIO.read(new ByteArrayInputStream(first.viewportPng())).getRGB(5, 5) & 0xffffff);
            assertEquals(0x0000ff, ImageIO.read(new ByteArrayInputStream(second.viewportPng())).getRGB(5, 5) & 0xffffff);
            assertEquals("BackCompat", session.page().evaluate("document.compatMode"));
            assertEquals(true, session.page().locator("input").evaluate("input=>input.readOnly"));
        }
    }

    @Test void preparationCannotMakeAReadonlyInputWritable() {
        try (var session = open(URI.create("about:blank"), 100)) {
            new PlaywrightPagePreparation(session.page(), Duration.ofSeconds(5))
                    .setContent(CONTROLS + "<script>entry.readOnly=true</script>");
            var actions = session.newActionSequence();
            assertThrows(PlaywrightException.class, () -> actions.fill(
                    new AtfLocator(session.page().locator("#entry"), "#entry"), "forbidden", "readonly must stay refused"));
            assertEquals("", session.page().locator("#entry").inputValue());
            assertEquals(0, session.page().evaluate("inputs"));
            assertEquals(1, actions.snapshot().size()); assertEquals("FAIL", actions.snapshot().getFirst().status().name());
        }
    }

    private static PlaywrightUiSession open(URI origin, int zoom) {
        return PlaywrightUiSession.open(new UiBrowserConfiguration(origin, "chromium", true,
                Duration.ofMillis(700), Duration.ofSeconds(5), 1000, 800, zoom, true));
    }
}
