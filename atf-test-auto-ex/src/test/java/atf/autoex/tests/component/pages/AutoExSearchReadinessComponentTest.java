package atf.autoex.tests.component.pages;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.runtime.catalog.AutoExCatalogSearch;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightUiSession;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import static atf.testkit.assertions.AuditedAssertions.*;

@AutoExComponentTest
class AutoExSearchReadinessComponentTest {
    @Test void decodedQueryAndDelayedVisibleProductConfirmOneSubmission() throws Exception {
        verify("Blue Top", true, true);
    }

    @Test void wrongLocationOrProductCannotPassAndCannotRepeatTheClick() throws Exception {
        verify("Other product", false, true);
        verify("Blue Top", false, false);
    }

    @Test void aSecondSearchAlsoFollowsTheReplacedDocumentWithoutAnotherActivation() throws Exception {
        verify("Blue Top", true, true, 2);
    }

    private static void verify(String product, boolean pass, boolean correctLocation) throws Exception {
        verify(product, pass, correctLocation, 1);
    }

    private static void verify(String product, boolean pass, boolean correctLocation, int searches) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String document = exchange.getRequestURI().getPath().equals("/start") ? """
                    <input id="search_product">
                    <button id="submit_search" onclick="sessionStorage.setItem('submissions',
                    Number(sessionStorage.getItem('submissions')||0)+1);location.href='/products?search='+%s">Search</button>
                    """.formatted(correctLocation ? "encodeURIComponent(document.querySelector('input').value)" : "'wrong'")
                    : """
                    <div class="features_items"><div class="productinfo"><p>stale catalog item</p></div></div>
                    <script>setTimeout(()=>document.querySelector('.productinfo p').textContent='%s',120)</script>
                    """.formatted(product);
            byte[] html = document.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, html.length); exchange.getResponseBody().write(html);
            }
        });
        server.start();
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (var browser = PlaywrightUiSession.open(new UiBrowserConfiguration(base, "chromium", true,
                Duration.ofMillis(700), Duration.ofSeconds(5), 1000, 800, 100, true))) {
            for (int index = 1; index <= searches; index++) {
                browser.page().navigate(base + "/start");
                var actions = browser.newActionSequence();
                actions.fill(new AtfLocator(browser.page().locator(AutoExSelectors.SEARCH_INPUT), AutoExSelectors.SEARCH_INPUT),
                        "Blue Top", "query entered");
                var search = new AutoExCatalogSearch(browser.page(), actions, base);
                if (pass) search.submit("Blue Top", "Blue Top");
                else {
                    var failure = assertThrows(IllegalStateException.class, () -> search.submit("Blue Top", "Blue Top"));
                    assertTrue(failure.getMessage().contains(correctLocation
                            ? "awaiting=visible first product Blue Top" : "awaiting=matching search URL"));
                    assertTrue(failure.getMessage().contains(correctLocation ? "search=Blue%20Top" : "<not observed>"));
                    assertNotNull(failure.getCause());
                }
                assertEquals(Integer.toString(index), browser.page().evaluate("sessionStorage.getItem('submissions')"));
                assertEquals(pass, actions.snapshot().getLast().status() == atf.core.reporting.contract.AtfReportStatus.PASS);
                assertEquals(2, actions.snapshot().size());
            }
        } finally { server.stop(0); }
    }
}
