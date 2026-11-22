package atf.ui.playwright;

import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Local pointer dispatch and readiness, without site-specific selectors or retries. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaywrightClickPostconditionComponentTest {
    private HttpServer server;
    private ExecutorService executor;
    private PlaywrightUiSession browser;
    private final CountDownLatch releaseFrame = new CountDownLatch(1);
    private final java.util.concurrent.atomic.AtomicInteger frameRequests = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicBoolean frameCommitted = new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicBoolean frameGateExpired = new java.util.concurrent.atomic.AtomicBoolean();
    private String origin;

    @BeforeAll void open() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool(); server.setExecutor(executor);
        server.createContext("/slow", exchange -> {
            frameRequests.incrementAndGet();
            try (exchange) {
                try {
                    if (!releaseFrame.await(5, TimeUnit.SECONDS)) {
                        frameGateExpired.set(true);
                        throw new java.io.IOException("Uncommitted-frame fixture gate was not released");
                    }
                }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("Uncommitted-frame fixture interrupted", interrupted);
                }
                frameCommitted.set(true);
                exchange.sendResponseHeaders(200, 0);
            }
        });
        server.createContext("/", exchange -> {
            byte[] document = """
                    <script>window.clicks=0;window.ready=false;</script>
                    <button id="activate" onclick="window.clicks++;window.trustedClick=event.isTrusted;const f=document.createElement('iframe');
                    f.src='/slow';document.body.append(f);setTimeout(()=>window.ready=true,80)">Activate</button>
                    """.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, document.length); exchange.getResponseBody().write(document);
            }
        });
        server.start(); origin = "http://127.0.0.1:" + server.getAddress().getPort();
        browser = PlaywrightUiSession.open(new UiBrowserConfiguration(URI.create(origin), "chromium", true,
                Duration.ofMillis(700), Duration.ofSeconds(5), 1000, 800, 100, true));
    }

    @AfterAll void close() {
        releaseFrame.countDown();
        try { if (browser != null) browser.close(); }
        finally { if (server != null) server.stop(0); if (executor != null) executor.shutdownNow(); }
    }

    @Test void explicitReadinessDoesNotWaitForAnUnrelatedUncommittedFrame() {
        try {
            preparation().navigate(origin + "/start");
            var actions = browser.newActionSequence();
            actions.clickAndAwait(target("#activate"), "ready state confirmed", budget -> {
                browser.page().waitForFunction("window.ready", null,
                        new com.microsoft.playwright.Page.WaitForFunctionOptions().setTimeout(budget.remainingMilliseconds())).dispose();
                browser.page().waitForCondition(() -> frameRequests.get() == 1,
                        new com.microsoft.playwright.Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
            });
            assertEquals(1, clicks());
            assertEquals(true, browser.page().evaluate("window.trustedClick"));
            assertEquals(2, browser.page().frames().size());
            assertFalse(frameCommitted.get(), "The child response is actually uncommitted when readiness returns");
            assertFalse(frameGateExpired.get(), "The fixture cannot silently complete its own negative condition");
            assertEquals(1, releaseFrame.getCount());
            assertEquals(AtfReportStatus.PASS, actions.snapshot().getFirst().status());
            assertTrue(actions.snapshot().getFirst().observed().contains("explicit postcondition satisfied"));
        } finally { releaseFrame.countDown(); }
    }

    @Test void unconfirmedReadinessKeepsFailureAndNeverReplaysTheClick() {
        preparation().setContent("<script>window.clicks=0;</script><button id='activate' onclick='window.clicks++'>Activate</button>");
        var actions = browser.newActionSequence();
        var failure = assertThrows(IllegalStateException.class,
                () -> actions.clickAndAwait(target("#activate"), "ready", budget -> {
                    browser.page().waitForFunction("false", null,
                            new com.microsoft.playwright.Page.WaitForFunctionOptions()
                                    .setTimeout(budget.remainingMilliseconds())).dispose();
                }));
        assertTrue(failure.getMessage().contains("Pointer click completed"));
        assertNotNull(failure.getCause());
        assertEquals(1, clicks());
        assertEquals(1, actions.snapshot().size());
        assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
    }

    @Test void absentOrAmbiguousTargetCannotRunThePostcondition() {
        for (String markup : new String[]{"<p>missing</p>", "<button>one</button><button>two</button>"}) {
            preparation().setContent(markup);
            var actions = browser.newActionSequence();
            var observations = new java.util.concurrent.atomic.AtomicInteger();
            assertThrows(RuntimeException.class, () -> actions.clickAndAwait(target("button"), "ready",
                    budget -> observations.incrementAndGet()));
            assertEquals(0, observations.get());
            assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
        }
    }

    private AtfLocator target(String selector) { return new AtfLocator(browser.page().locator(selector), selector); }
    private PlaywrightPagePreparation preparation() { return new PlaywrightPagePreparation(browser.page(), Duration.ofSeconds(5)); }
    private int clicks() { return ((Number) browser.page().evaluate("window.clicks")).intValue(); }
}
