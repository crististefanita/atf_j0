package atf.autoex.tests.component.pages;

import atf.autoex.runtime.catalog.AutoExCatalogSearch;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.playwright.PlaywrightPagePreparation;
import atf.core.reporting.contract.AtfReportStatus;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Response/DOM ownership and terminal failures are independent of a later healthy catalog API. */
@AutoExComponentTest
class AutoExSearchResponseComponentTest {
    @ParameterizedTest @ValueSource(ints = {200, 503})
    void queueFullDocumentFailsAsUnavailableInsteadOfWaitingForAProduct(int status) throws Exception {
        verify(status, "<h3>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h3>", Mode.DOCUMENT, false,
                status == 200 ? "queue full" : "HTTP " + status);
    }
    @Test void unsuccessfulHttpCannotPassEvenWithTheExpectedProductInTheBody() throws Exception {
        verify(525, product(), Mode.DOCUMENT, false, "HTTP 525");
    }
    @Test void aHistoryOnlyUrlChangeAndStaleProductCannotPretendToBeASearchResponse() throws Exception {
        verify(200, product(), Mode.HISTORY, false, "document");
    }
    @Test void aSuccessfulOwnedDocumentConfirmsOneClickAndOneGet() throws Exception {
        verify(200, product(), Mode.DOCUMENT, true, "HTTP 200");
    }
    @Test void visibleBusyMarkerWinsEvenWhenTheExpectedProductAlsoExists() throws Exception {
        verify(200, product() + "<h3>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h3>", Mode.DOCUMENT, false, "queue full");
    }
    @Test void aHiddenMarkerDoesNotInvalidateAHealthyProduct() throws Exception {
        verify(200, product() + "<h3 hidden>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h3>", Mode.DOCUMENT, true, "HTTP 200");
    }
    @Test void aHealthyJsonApiCannotCertifyAnAbsentDocument() throws Exception {
        verify(200, product(), Mode.API_ONLY, false, "document");
    }
    @Test void aHealthyJsonApiCannotCertifyTheFailedHtmlDocument() throws Exception {
        verify(503, product(), Mode.API_THEN_DOCUMENT, false, "HTTP 503");
    }
    @Test void transportFailureIsExplicitAndDoesNotRepeatThePointer() throws Exception {
        verify(200, product(), Mode.ABORT_DOCUMENT, false, "Search transport failed");
    }
    @Test void aHealthyProductRetainsExactTextWhitespaceNormalization() throws Exception {
        verify(200, product().replace("Blue Top", "Blue&nbsp;\nTop"), Mode.DOCUMENT, true, "HTTP 200");
    }
    @Test void whitespaceInsideTheBusyMessageDoesNotTurnItIntoAProductTimeout() throws Exception {
        verify(200, "<h3>" + AutoExSelectors.STORE_BUSY_MESSAGE.replace("under heavy", "under\n  heavy") + "</h3>",
                Mode.DOCUMENT, false, "queue full");
    }

    private static void verify(int status, String result, Mode mode, boolean pass, String reason) throws Exception {
        var documents = new AtomicInteger();
        var apis = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body = switch (path) {
                case "/start" -> product() + "<input id='search_product' value='Blue Top'>"
                        + "<button id='submit_search' onclick=\"" + mode.script + "\">Search</button>";
                case "/api/searchProduct" -> { apis.incrementAndGet(); yield "{\"responseCode\":200,\"products\":[{\"name\":\"Blue Top\"}]}"; }
                default -> { documents.incrementAndGet(); yield result; }
            };
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", path.startsWith("/api/") ? "application/json" : "text/html; charset=utf-8");
                exchange.sendResponseHeaders(path.equals("/products") ? status : 200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        URI origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (var ui = PlaywrightUiSession.open(new UiBrowserConfiguration(origin, "chromium", true,
                Duration.ofMillis(700), Duration.ofSeconds(5), 1000, 800, 100, true))) {
            var prepared = new PlaywrightPagePreparation(ui.page(), Duration.ofSeconds(5)).navigate(origin + "/start");
            var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(prepared.viewportPng()));
            assertTrue(image != null, "Fixture setup produces a decoded native viewport before measured search");
            assertEquals(origin + "/start", prepared.url(), "Prepared document belongs to this fixture");
            assertEquals(1000, image.getWidth(), "Prepared viewport width");
            assertEquals(800, image.getHeight(), "Prepared viewport height");
            var clicks = new AtomicInteger();
            var trusted = new AtomicInteger();
            ui.page().exposeFunction("recordSearchClick", arguments -> {
                clicks.incrementAndGet(); if (Boolean.TRUE.equals(arguments[0])) trusted.incrementAndGet(); return null;
            });
            ui.page().locator("#submit_search").evaluate("element=>element.addEventListener('click',event=>recordSearchClick(event.isTrusted))");
            if (mode == Mode.ABORT_DOCUMENT) ui.page().route("**/products?*", com.microsoft.playwright.Route::abort);
            var actions = ui.newActionSequence();
            var search = new AutoExCatalogSearch(ui.page(), actions, origin);
            if (pass) search.submit("Blue Top", "Blue Top");
            else {
                var failure = assertThrows(IllegalStateException.class, () -> search.submit("Blue Top", "Blue Top"));
                String chain = causes(failure);
                assertTrue(chain.contains(reason), chain);
                if (reason.equals("queue full") || status != 200 || mode == Mode.ABORT_DOCUMENT)
                    assertFalse(chain.contains("TimeoutError"), "A terminal server outcome must not become a locator timeout");
            }
            assertEquals(1, clicks.get(), "Exactly one native pointer reaches the page");
            assertEquals(1, trusted.get(), "The search is a trusted native activation");
            assertEquals(mode == Mode.DOCUMENT || mode == Mode.API_THEN_DOCUMENT ? 1 : 0, documents.get(), "Owned HTML GET count");
            assertEquals(mode == Mode.API_ONLY || mode == Mode.API_THEN_DOCUMENT ? 1 : 0, apis.get(), "Independent JSON API count");
            assertEquals(pass ? AtfReportStatus.PASS : AtfReportStatus.FAIL, actions.snapshot().getLast().status());
            var observation = actions.snapshot().getFirst();
            assertEquals("Catalog search response", observation.operation());
            assertEquals(AtfReportStatus.INFO, observation.status(), "HTTP/DOM evidence does not fabricate a business assertion");
            if (mode == Mode.HISTORY || mode == Mode.API_ONLY || mode == Mode.ABORT_DOCUMENT)
                assertTrue(observation.observed().contains("HTTP unobserved"), "An absent response must not appear as an HTTP status of zero");
        } finally { server.stop(0); }
    }
    private enum Mode {
        DOCUMENT("location.href='/products?search=Blue%20Top'"),
        HISTORY("history.pushState({},'','/products?search=Blue%20Top')"),
        API_ONLY("fetch('/api/searchProduct').then(()=>history.pushState({},'','/products?search=Blue%20Top'))"),
        API_THEN_DOCUMENT("fetch('/api/searchProduct').then(()=>location.href='/products?search=Blue%20Top')"),
        ABORT_DOCUMENT("location.href='/products?search=Blue%20Top'");
        final String script;
        Mode(String script) { this.script = script; }
    }
    private static String product() { return "<div class='features_items'><div class='productinfo'><p>Blue Top</p></div></div>"; }
    private static String causes(Throwable failure) {
        var out = new StringBuilder();
        for (Throwable next = failure; next != null; next = next.getCause()) out.append(next.getClass().getName()).append(':').append(next.getMessage()).append('\n');
        return out.toString();
    }
}
