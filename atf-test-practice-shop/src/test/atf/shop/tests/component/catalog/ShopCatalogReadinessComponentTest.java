package atf.shop.tests.component.catalog;

import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopHomePage;
import atf.shop.support.catalog.ShopCatalogReadiness;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.catalog.ShopCatalogReadinessFixture;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.*;
import com.microsoft.playwright.TimeoutError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.parallel.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import com.microsoft.playwright.Route;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class ShopCatalogReadinessComponentTest {
    private PlaywrightUiSession browser;
    private ShopCatalogReadinessFixture active;
    private final ShopLocators locators = new ShopLocators();

    @BeforeAll void openBrowser() {
        browser = PlaywrightUiSession.open(new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofMillis(850), Duration.ofSeconds(10), 1280, 900, 100, true), event -> {
                    if (active != null && event.type() == atf.core.reporting.console.AtfConsoleEvent.Type.START
                            && event.name().startsWith("Await visible ->")) active.readinessStarted();
                });
    }
    @AfterAll void closeBrowser() { if (browser != null) browser.close(); }

    @ParameterizedTest @ValueSource(strings = {"GET", "QUERY"})
    void successfulReadWaitsForTheActualGridWithoutRefresh(String method) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("delayed")) {
            var actions = open(fixture, method);
            assertEquals(1, fixture.documents.get());assertEquals(1, fixture.requests.get());
            assertEquals(List.of(method), fixture.methods);assertEquals(0L, refreshes(actions));
            assertEquals(true, locators.homeProductGridFirst(browser.page()).locator().isVisible());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "QUERY"})
    void transientReadRefreshesOnlyHomeAndRetainsTheOriginalMethodAndBody(String method) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("transient")) {
            var actions = open(fixture, method);
            assertEquals(2, fixture.documents.get());assertEquals(2, fixture.requests.get());
            assertEquals(List.of(method, method), fixture.methods);
            String body = method.equals("QUERY") ? "{\"page\":1,\"sort\":\"name,asc\"}" : "";
            assertEquals(List.of(body, body), fixture.bodies);assertEquals(1L, refreshes(actions));
            assertTrue(actions.snapshot().stream().anyMatch(a -> a.observed().contains("status=503")));
            assertTrue(actions.snapshot().stream().anyMatch(a -> a.observed().contains("status=200")));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "QUERY"})
    void repeatedTransientFailureStopsAfterExactlyOneRefresh(String method) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("exhausted")) {
            var actions = browser.newActionSequence();
            var failure = assertThrows(IllegalStateException.class, () -> load(fixture, method, actions));
            assertTrue(failure.getMessage().contains("HTTP 503"));assertEquals(2, fixture.documents.get());
            assertEquals(List.of(method, method), fixture.methods);assertEquals(1L, refreshes(actions));
            assertTrue(actions.snapshot().stream().anyMatch(a -> a.operation().equals("Await visible")
                    && a.status().name().equals("FAIL")));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "QUERY"})
    void successfulHttpWithoutProductsStillTimesOutAndNeverRefreshes(String method) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("empty")) {
            var actions = browser.newActionSequence();
            assertThrows(TimeoutError.class, () -> load(fixture, method, actions));
            assertEquals(1, fixture.documents.get());assertEquals(1, fixture.requests.get());assertEquals(0L, refreshes(actions));
            assertTrue(actions.snapshot().stream().anyMatch(a -> a.operation().equals("Catalog readiness failed")
                    && a.observed().contains("products=0") && a.observed().contains("status=200")));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "QUERY"})
    void permanentResponseIsRefusedWithoutReload(String method) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("forbidden")) {
            var actions = browser.newActionSequence();
            assertTrue(assertThrows(IllegalStateException.class, () -> load(fixture, method, actions))
                    .getMessage().contains("HTTP 403"));
            assertEquals(1, fixture.requests.get());assertEquals(1, fixture.documents.get());assertEquals(0L, refreshes(actions));
        }
    }

    @Test void retryAfterCannotExtendTheOriginalDeadline() throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("retry-after")) {
            var actions = browser.newActionSequence();
            assertTrue(assertThrows(TimeoutError.class, () -> load(fixture, "GET", actions)).getMessage().contains("Retry-After"));
            assertEquals(1, fixture.requests.get());assertEquals(1, fixture.documents.get());assertEquals(0L, refreshes(actions));
        }
    }

    @Test void refreshAndSecondDomWaitShareOneBudget() throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("budget")) {
            var actions = browser.newActionSequence();
            assertThrows(TimeoutError.class, () -> load(fixture, "QUERY", actions));
            assertEquals(2, fixture.requests.get());assertEquals(2, fixture.documents.get());
            var wait = actions.snapshot().stream().filter(a -> a.operation().equals("Await visible")).findFirst().orElseThrow();
            assertTrue(wait.durationMillis() < 1100);assertEquals("FAIL", wait.status().name());
            assertEquals(1L, actions.snapshot().stream().filter(a -> a.operation().equals("Await visible")).count());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"post", "unrelated"})
    void aWriteOrAnotherEndpointCannotAuthorizeCatalogRefresh(String mode) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture(mode)) {
            var actions = browser.newActionSequence();
            assertThrows(TimeoutError.class, () -> load(fixture, mode.equals("post") ? "POST" : "GET", actions));
            assertEquals(1, fixture.requests.get());assertEquals(1, fixture.documents.get());assertEquals(0L, refreshes(actions));
        }
    }

    @Test void documentChangeIsRefusedRatherThanRefreshed() throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("redirect")) {
            var actions = browser.newActionSequence();
            assertTrue(assertThrows(IllegalStateException.class, () -> load(fixture, "GET", actions))
                    .getMessage().contains("different document"));
            assertEquals(1, fixture.documents.get());assertEquals(0L, refreshes(actions));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"/invoices", "/payment/check", "/products/search", "/products?other=1", "/products#other"})
    void aDifferentOrAmbiguousRouteIsRejectedBeforeAnyBrowserWork(String path) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("empty")) {
            assertThrows(IllegalArgumentException.class, () -> new ShopCatalogReadiness(browser.page(), fixture.home("GET"),
                    fixture.endpoint().resolve(path), browser.newActionSequence()));
            assertEquals(0, fixture.documents.get());assertEquals(0, fixture.requests.get());
        }
    }

    @Test void anUnownedReloadCannotUseTheSameUrlToReplaceTheCurrentDocument() throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("unexpected")) {
            var actions = browser.newActionSequence();
            assertTrue(assertThrows(IllegalStateException.class, () -> load(fixture, "GET", actions))
                    .getMessage().contains("different document"));
            assertEquals(2, fixture.documents.get());assertEquals(0L, refreshes(actions));
        }
    }

    @Test void theSameObservationCannotAcquireAnotherReadinessDeadline() throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("delayed")) {
            browser.page().navigate(fixture.home("GET").toString());
            var actions = browser.newActionSequence();var product = locators.homeProductGridFirst(browser.page());
            try (var catalog = new ShopCatalogReadiness(browser.page(), fixture.home("GET"), fixture.endpoint(), actions)) {
                actions.awaitVisible(product, budget -> catalog.await(product.locator(), budget));
                assertThrows(IllegalStateException.class,
                        () -> actions.awaitVisible(product, budget -> catalog.await(product.locator(), budget)));
                assertEquals(1, fixture.documents.get());assertEquals(1, fixture.requests.get());
            }
        }
    }

    @Test void originComparisonUsesHttpSemanticsRatherThanTheRawAuthoritySpelling() throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("transient")) {
            var home = URI.create(fixture.home("QUERY").toString().replace("http:", "HTTP:"));
            var endpoint = URI.create(fixture.endpoint().toString().replace("http:", "HTTP:"));
            var actions = browser.newActionSequence();
            new ShopHomePage(browser.page(), locators, actions).open(home, endpoint);
            assertEquals(2, fixture.documents.get());assertEquals(List.of("QUERY", "QUERY"), fixture.methods);
            assertEquals(1L, refreshes(actions));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "QUERY"})
    void aConfirmedConnectionResetCanRefreshTheReadWithoutChangingItsMethod(String method) throws Exception {
        try (var fixture = new ShopCatalogReadinessFixture("reset")) {
            var intercepted = new AtomicInteger();
            Consumer<Route> route = request -> {
                if (intercepted.incrementAndGet() == 1) request.abort("connectionreset");else request.resume();
            };
            browser.page().route(fixture.endpoint().toString(), route);
            try {
                var actions = open(fixture, method);
                assertEquals(2, intercepted.get());assertEquals(2, fixture.documents.get());
                assertEquals(List.of(method), fixture.methods);assertEquals(1L, refreshes(actions));
                assertTrue(actions.snapshot().stream().anyMatch(a -> a.observed().contains("ERR_CONNECTION_RESET")));
                assertTrue(actions.snapshot().stream().anyMatch(a -> a.observed().contains("status=unavailable")));
                assertFalse(actions.snapshot().stream().anyMatch(a -> a.observed().contains("status=0")));
            } finally {browser.page().unroute(fixture.endpoint().toString(), route);}
        }
    }

    private PlaywrightActionSequence open(ShopCatalogReadinessFixture fixture, String method) {
        var actions = browser.newActionSequence();load(fixture, method, actions);return actions;
    }
    private void load(ShopCatalogReadinessFixture fixture, String method, PlaywrightActionSequence actions) {
        active = fixture;
        try {new ShopHomePage(browser.page(), locators, actions).open(fixture.home(method), fixture.endpoint());}
        finally {active = null;}
    }
    private static long refreshes(PlaywrightActionSequence actions) {
        return actions.snapshot().stream().filter(a -> a.operation().equals("Retry catalog home GET")).count();
    }
}
