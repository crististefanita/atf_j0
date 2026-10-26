package atf.ui.network;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class UiNetworkInspectorComponentTest {
    @Test
    void browserSelectsSuccessfulAndFailedFetchAndReportsOverflowAfterStaticTraffic() {
        String scripts = java.util.stream.IntStream.range(0, 32)
                .mapToObj(index -> "<script src='/static/" + index + ".js'></script>")
                .collect(java.util.stream.Collectors.joining());
        try (var session = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
             var inspector = new UiNetworkInspector(new UiNetworkInspectionConfiguration(true,
                     Set.of("network-fixture.test"), 2, 256, true, UiNetworkSettlementPolicy.disabled(),
                     UiNetworkCapturePolicy.asynchronous()))) {
            var page = session.page();
            String base = "http://network-fixture.test";
            page.route(base + "/**", route -> {
                String path = java.net.URI.create(route.request().url()).getPath();
                if (path.equals("/failed")) {
                    route.abort();
                    return;
                }
                String body = path.equals("/") ? "<html><head>" + scripts + "</head><body>fixture</body></html>"
                        : path.startsWith("/static/") ? "/* static asset */" : "browser response";
                route.fulfill(new com.microsoft.playwright.Route.FulfillOptions().setStatus(200)
                        .setContentType(path.endsWith(".js") ? "application/javascript" : "text/html")
                        .setBody(body));
            });
            inspector.attach(page);
            page.navigate(base);
            page.evaluate("async base => (await fetch(base + '/success')).text()", base);
            page.waitForCondition(() -> inspector.statistics().accepted() == 1);
            page.evaluate("""
                    async base => { try { await fetch(base + '/failed'); } catch (expected) { } }
                    """, base);
            page.waitForCondition(() -> inspector.statistics().accepted() == 2);
            page.evaluate("async base => (await fetch(base + '/overflow')).text()", base);
            page.waitForCondition(() -> inspector.statistics().accepted() == 3);
            var observations = inspector.observations();
            var coverage = inspector.statistics();
            assertAll(
                    () -> assertEquals(2, observations.size()),
                    () -> assertEquals(200, observations.getFirst().status()),
                    () -> assertEquals("browser response", observations.getFirst().responseBody()),
                    () -> assertTrue(observations.getLast().requestFailed()),
                    () -> assertEquals(UiNetworkCapturePolicy.ResourceKind.FETCH, observations.getLast().resourceKind()),
                    () -> assertEquals(3, coverage.accepted()),
                    () -> assertEquals(1, coverage.dropped()),
                    () -> assertTrue(coverage.excluded() >= 32),
                    () -> assertTrue(coverage.limitReached()),
                    () -> assertFalse(coverage.complete()),
                    () -> assertEquals(List.of(), inspector.observationsSince(2)));
        }
    }

    @Test
    void assetsExceedingTheBudgetCannotEvictAnHtmlXhrOrReadTheirBodies() {
        var inspector = inspector(2, UiNetworkCapturePolicy.asynchronous());
        var reads = new AtomicInteger();
        for (int index = 0; index < 300; index++) {
            assertTrue(inspector.capture(response("/asset-" + index, "image", reads)).isEmpty());
        }
        var captured = inspector.capture(response("/cart/add/1", "xhr", reads)).orElseThrow();

        assertAll(
                () -> assertEquals(1, reads.get()),
                () -> assertEquals(UiNetworkCapturePolicy.ResourceKind.XHR, captured.resourceKind()),
                () -> assertEquals("text/html", captured.responseHeaders().get("content-type")),
                () -> assertEquals("<p>accepted</p>", captured.responseBody()),
                () -> assertEquals(300, inspector.statistics().excluded()),
                () -> assertEquals(1, inspector.statistics().accepted()),
                () -> assertTrue(inspector.statistics().complete()));
    }

    @Test
    void overflowIsObservableBeforeResponseBodyAccessAndCursorsStayAppendOnly() {
        var inspector = inspector(2, UiNetworkCapturePolicy.asynchronous());
        var reads = new AtomicInteger();
        inspector.capture(response("/setup", "fetch", reads)).orElseThrow();
        int baseline = inspector.observationCursor();
        assertTrue(inspector.observationsSince(baseline).isEmpty());
        inspector.capture(response("/cart/add/1", "xhr", reads)).orElseThrow();
        assertTrue(inspector.capture(response("/checkout/readback", "fetch", reads)).isEmpty());

        assertAll(
                () -> assertEquals(List.of("/cart/add/1"),
                        inspector.observationsSince(baseline).stream().map(UiNetworkObservation::path).toList()),
                () -> assertEquals(2, reads.get()),
                () -> assertEquals(new UiNetworkCaptureStatistics(3, 2, 0, 1, 0, true), inspector.statistics()),
                () -> assertFalse(inspector.statistics().complete()),
                () -> assertThrows(IllegalArgumentException.class, () -> inspector.observationsSince(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> inspector.observationsSince(3)),
                () -> assertThrows(UnsupportedOperationException.class, () -> inspector.observationsSince(0).clear()));
    }

    @Test
    void documentCaptureIsExplicitAndPathAndHostBoundariesApplyBeforeBodies() {
        var reads = new AtomicInteger();
        var defaultInspector = inspector(4, UiNetworkCapturePolicy.asynchronous());
        assertTrue(defaultInspector.capture(response("/checkout", "document", reads)).isEmpty());
        var inspector = inspector(4, UiNetworkCapturePolicy.asynchronous().includingDocuments()
                .forPaths(List.of("/checkout", "/cart")));
        assertTrue(inspector.capture(response("/checkout", "document", reads)).isPresent());
        assertTrue(inspector.capture(response("/cart/add/1", "xhr", reads)).isPresent());
        assertTrue(inspector.capture(response("/unrelated", "fetch", reads)).isEmpty());
        assertTrue(inspector.capture(response("/checkout/app.js", "script", reads)).isEmpty());
        assertTrue(inspector.capture(response("https://third-party.test/cart", "xhr", reads)).isEmpty());
        assertEquals(2, reads.get());
        assertEquals(3, inspector.statistics().excluded());
        assertFalse(inspector.configuration().capturePolicy().description().contains("/checkout"));
        assertThrows(IllegalArgumentException.class,
                () -> UiNetworkCapturePolicy.asynchronous().forPaths(List.of("/cart?token=secret")));
    }

    @Test
    void selectedDocumentRoutesDoNotNarrowAsynchronousApiCaptureOrMatchNeighborRoutes() {
        var policy = UiNetworkCapturePolicy.asynchronous()
                .includingDocumentsFor(List.of("/login", "/delete_account"));
        var inspector = inspector(4, policy);
        var bodyReads = new AtomicInteger();
        var login = inspector.capture(response("/login", "document", bodyReads)).orElseThrow();
        var xhr = inspector.capture(response("/add_to_cart/1", "xhr", bodyReads)).orElseThrow();

        assertAll(
                () -> assertTrue(policy.accepts("https://service.test/login", UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertTrue(policy.accepts("https://service.test/login?next=%2F", UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertTrue(policy.accepts("https://service.test/delete_account", UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertFalse(policy.accepts("https://service.test/", UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertFalse(policy.accepts("https://service.test/products", UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertFalse(policy.accepts("https://service.test/login-extra", UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertTrue(policy.accepts("https://service.test/api/createAccount", UiNetworkCapturePolicy.ResourceKind.FETCH)),
                () -> assertTrue(policy.accepts("https://service.test/add_to_cart/1", UiNetworkCapturePolicy.ResourceKind.XHR)),
                () -> assertFalse(policy.accepts("https://service.test/login", UiNetworkCapturePolicy.ResourceKind.STATIC)),
                () -> assertFalse(policy.capturesResponseBody(UiNetworkCapturePolicy.ResourceKind.DOCUMENT)),
                () -> assertTrue(policy.capturesResponseBody(UiNetworkCapturePolicy.ResourceKind.XHR)),
                () -> assertFalse(login.responseBodyAvailable()),
                () -> assertEquals("", login.responseBody()),
                () -> assertTrue(xhr.responseBodyAvailable()),
                () -> assertEquals("<p>accepted</p>", xhr.responseBody()),
                () -> assertEquals(1, bodyReads.get(), "selected document HTML is not read"),
                () -> assertTrue(policy.scope().contains("documentPaths=[/login, /delete_account]")),
                () -> assertTrue(policy.description().contains("selected document routes")),
                () -> assertTrue(policy.description().contains("response bodies=fetch, xhr")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> UiNetworkCapturePolicy.asynchronous().includingDocumentsFor(List.of("/login?next=/"))));
    }

    @Test
    void failedXhrKeepsRedactedTypedEvidenceAndDoesNotCollapseRawRetryObservations() {
        var finished = new AtomicReference<Consumer<Request>>();
        var failed = new AtomicReference<Consumer<Request>>();
        Page page = proxy(Page.class, (name, args) -> {
            if (name.equals("onRequestFinished")) finished.set(callback(args));
            if (name.equals("onRequestFailed")) failed.set(callback(args));
            return null;
        });
        Request request = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/cart?token=secret-value";
            case "resourceType" -> "xhr";
            case "method" -> "POST";
            case "headers" -> Map.of("Authorization", "Bearer secret-value");
            case "postData" -> "{\"token\":\"secret-value\"}";
            case "failure" -> "net::ERR_FAILED https://service.test/cart?token=secret-value";
            default -> null;
        });
        var inspector = inspector(4, UiNetworkCapturePolicy.asynchronous());
        var received = new ArrayList<UiNetworkObservation>();
        inspector.attach(page, received::add);
        failed.get().accept(request);
        failed.get().accept(request);
        var observation = received.getFirst();

        assertAll(
                () -> assertTrue(observation.requestFailed()),
                () -> assertEquals(UiNetworkCapturePolicy.ResourceKind.XHR, observation.resourceKind()),
                () -> assertEquals("bearer", observation.authMethod()),
                () -> assertEquals("request-header", observation.authSource()),
                () -> assertFalse(observation.toString().contains("secret-value")),
                () -> assertEquals(2, inspector.observations().size()),
                () -> assertEquals(1, inspector.presentationObservations().size()),
                () -> assertEquals(2, inspector.signatureCounts().values().iterator().next()),
                () -> assertTrue(inspector.statistics().complete()));
        inspector.close();
    }

    @Test
    void failedCaptureReleasesCapacityButRemainsIncompleteAndSubscribersCannotRewriteRetention() {
        var inspector = inspector(1, UiNetworkCapturePolicy.asynchronous());
        Request broken = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/cart";
            case "resourceType" -> "xhr";
            case "postData" -> throw new IllegalStateException("browser context replaced");
            default -> null;
        });
        Response brokenResponse = proxy(Response.class,
                (name, args) -> name.equals("request") ? broken : null);
        assertThrows(IllegalStateException.class, () -> inspector.capture(brokenResponse));
        inspector.subscribe(ignored -> { throw new IllegalStateException("consumer failure"); });
        assertThrows(IllegalStateException.class,
                () -> inspector.capture(response("/cart", "fetch", new AtomicInteger())));
        assertEquals(new UiNetworkCaptureStatistics(2, 1, 0, 1, 0, false), inspector.statistics());
        assertFalse(inspector.statistics().complete());
    }

    @Test
    void reentrantCaptureReservesBudgetBeforeBodyCapture() {
        var inspector = inspector(1, UiNetworkCapturePolicy.asynchronous());
        var reads = new AtomicInteger();
        Response nested = response("/nested", "xhr", reads);
        Request request = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/outer";
            case "resourceType" -> "fetch";
            case "method" -> "GET";
            case "headers" -> Map.of();
            case "postData" -> {
                assertEquals(1, inspector.statistics().inFlight());
                assertTrue(inspector.capture(nested).isEmpty());
                yield "";
            }
            default -> null;
        });
        Response outer = proxy(Response.class, (name, args) -> switch (name) {
            case "request" -> request;
            case "status" -> 200;
            case "body" -> new byte[0];
            case "headers" -> Map.of();
            default -> null;
        });
        inspector.capture(outer).orElseThrow();
        assertEquals(0, reads.get());
        assertEquals(new UiNetworkCaptureStatistics(2, 1, 0, 1, 0, true), inspector.statistics());
    }

    private static UiNetworkInspector inspector(int limit, UiNetworkCapturePolicy policy) {
        return new UiNetworkInspector(new UiNetworkInspectionConfiguration(true,
                Set.of("service.test"), limit, 256, true, UiNetworkSettlementPolicy.disabled(), policy));
    }

    @Test
    void completeBrowserHeadersDetermineObservedAuthBeforeCookieRedaction() {
        String cookie = "session-value-42";
        var reads = new AtomicInteger();
        Request request = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/cart";
            case "resourceType" -> "xhr";
            case "method" -> "GET";
            case "headers" -> Map.of();
            case "allHeaders" -> {
                reads.incrementAndGet();
                yield Map.of("Cookie", "SESSIONID=" + cookie,
                        ":path", "/cart?token=" + cookie, ":method", "GET");
            }
            default -> null;
        });
        Response response = proxy(Response.class, (name, args) -> switch (name) {
            case "request" -> request;
            case "status" -> 200;
            case "headers" -> Map.of();
            case "body" -> new byte[0];
            default -> null;
        });
        var captured = inspector(1, UiNetworkCapturePolicy.asynchronous()).capture(response).orElseThrow();
        assertEquals("cookie", captured.authMethod());
        assertEquals("request-header", captured.authSource());
        assertEquals(1, reads.get());
        assertFalse(captured.toString().contains(cookie));
        assertTrue(captured.requestHeaders().keySet().stream().noneMatch(key -> key.startsWith(":")),
                "Pseudo-headers must not bypass URL redaction or become invalid curl headers");
    }

    @Test
    void optionalFormCaptureRedactsCredentialsAndResponseCookiesBeforePublication() {
        String cookie = "cookie-value-42";
        String password = "Pa55-word";
        String form = "item=1&password=" + password;
        Request request = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/checkout";
            case "resourceType" -> "document";
            case "method" -> "POST";
            case "headers" -> Map.of("Cookie", "SESSIONID=" + cookie,
                    "Proxy-Authorization", "Basic " + password);
            case "postData" -> form;
            default -> null;
        });
        Response response = proxy(Response.class, (name, args) -> switch (name) {
            case "request" -> request;
            case "status" -> 200;
            case "headers" -> Map.of("Set-Cookie", "SESSIONID=" + cookie + "; HttpOnly; Secure",
                    "Location", "/callback?code=" + password,
                    "Content-Location", "//service.test/receipt?code=" + password);
            case "body" -> new byte[0];
            default -> null;
        });
        var inspector = inspector(1, UiNetworkCapturePolicy.asynchronous().includingDocuments());
        var received = new ArrayList<UiNetworkObservation>();
        inspector.subscribe(received::add);
        var captured = inspector.capture(response).orElseThrow();
        assertAll(
                () -> assertEquals(UiNetworkCapturePolicy.ResourceKind.DOCUMENT, captured.resourceKind()),
                () -> assertEquals("cookie", captured.authMethod()),
                () -> assertEquals("REDACTED", captured.responseHeaders().get("Set-Cookie")),
                () -> assertTrue(captured.responseHeaders().get("Content-Location").startsWith("//service.test/receipt")),
                () -> assertFalse(captured.toString().contains(cookie)),
                () -> assertFalse(captured.toString().contains(password)),
                () -> assertTrue(captured.requestBody().contains("item=1")),
                () -> assertTrue(captured.requestBody().contains("password=***REDACTED***")),
                () -> assertEquals(form, request.postData(), "Redaction must not mutate the real request"),
                () -> assertEquals(captured, received.getFirst()));
    }

    @Test
    void typedObservationCannotLeakCredentialsInUrlPathOrSignature() {
        String secret = "private-value-42";
        String url = "https://service.test/token/" + secret;
        var raw = new UiNetworkObservation(1, java.time.Instant.EPOCH, "GET", url,
                "/token/" + secret, 200, Map.of(), Map.of(), "", "", "none", "none",
                "service.test", "fixture", "untrusted", true, "",
                UiNetworkCapturePolicy.ResourceKind.XHR);
        var inspector = inspector(1, UiNetworkCapturePolicy.asynchronous());
        var captured = inspector.capture(raw).orElseThrow();
        assertFalse(captured.toString().contains(secret));
        assertTrue(captured.url().contains("/token/"));
        assertTrue(raw.url().contains(secret), "Only the published snapshot is redacted");
    }

    @Test
    void unavailableBodyDoesNotDiscardTheResponseOrPretendItWasCaptured() {
        var reads = new AtomicInteger();
        Response original = response("/cart/add/1", "xhr", reads);
        Response unavailable = proxy(Response.class, (name, args) -> switch (name) {
            case "request" -> original.request();
            case "status" -> 200;
            case "headers" -> Map.of("content-type", "text/html");
            case "body" -> throw new IllegalStateException("body no longer available");
            default -> null;
        });
        var inspector = inspector(1, UiNetworkCapturePolicy.asynchronous());
        var captured = inspector.capture(unavailable).orElseThrow();
        assertFalse(captured.responseBodyAvailable());
        assertEquals(200, captured.status());
        assertEquals(UiNetworkCapturePolicy.ResourceKind.XHR, captured.resourceKind());
        assertTrue(inspector.statistics().complete(), "The exchange is retained even when its body is unavailable");
    }

    @Test
    void finishedListenerRejectsAssetsAndOverflowBeforeObtainingTheResponse() {
        var listener = new AtomicReference<Consumer<Request>>();
        Page page = proxy(Page.class, (name, args) -> {
            if (name.equals("onRequestFinished")) listener.set(callback(args));
            return null;
        });
        var inspector = inspector(1, UiNetworkCapturePolicy.asynchronous());
        inspector.attach(page);
        var responseReads = new AtomicInteger();
        Request asset = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/image.png";
            case "resourceType" -> "image";
            case "response" -> { responseReads.incrementAndGet(); throw new AssertionError("excluded response read"); }
            default -> null;
        });
        listener.get().accept(asset);
        inspector.capture(response("/first", "xhr", new AtomicInteger())).orElseThrow();
        Request overflow = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> "https://service.test/later";
            case "resourceType" -> "xhr";
            case "response" -> { responseReads.incrementAndGet(); throw new AssertionError("overflow response read"); }
            default -> null;
        });
        listener.get().accept(overflow);
        assertEquals(0, responseReads.get());
        assertEquals(new UiNetworkCaptureStatistics(2, 1, 1, 1, 0, true), inspector.statistics());
    }

    private static Response response(String path, String resourceType, AtomicInteger bodyReads) {
        String url = path.startsWith("https://") ? path : "https://service.test" + path;
        Request request = proxy(Request.class, (name, args) -> switch (name) {
            case "url" -> url;
            case "resourceType" -> resourceType;
            case "method" -> "GET";
            case "headers" -> Map.of();
            default -> null;
        });
        return proxy(Response.class, (name, args) -> switch (name) {
            case "request" -> request;
            case "url" -> url;
            case "status" -> 200;
            case "headers" -> Map.of("content-type", "text/html");
            case "body" -> { bodyReads.incrementAndGet(); yield "<p>accepted</p>".getBytes(java.nio.charset.StandardCharsets.UTF_8); }
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Request> callback(Object[] args) { return (Consumer<Request>) args[0]; }

    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> body) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> body.apply(method.getName(), args)));
    }
}
