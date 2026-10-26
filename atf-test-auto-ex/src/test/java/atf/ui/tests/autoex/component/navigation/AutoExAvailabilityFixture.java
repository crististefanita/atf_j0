package atf.ui.tests.autoex.component.navigation;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Browser-intercepted document responses; no public endpoint or server process is used. */
final class AutoExAvailabilityFixture implements AutoCloseable {
    private static final String BUSY = "<main><h2>This website is under heavy load (queue full)</h2></main>"
            + "<p>Please try again later.</p>";
    private static final String READY = "<h1 id='ready'>Catalog ready</h1>";
    private final Page page;
    private final URI origin;
    private final String pattern;
    private final Consumer<Route> responder;
    private final List<String> calls = new ArrayList<>();

    AutoExAvailabilityFixture(Page page, int busyResponses) {
        this(page, busyResponses, "autoex.fixture.test");
    }

    AutoExAvailabilityFixture(Page page, int busyResponses, String host) {
        this.page = page;
        origin = URI.create("https://" + host);
        pattern = origin + "/**";
        responder = route -> {
            URI target = URI.create(route.request().url());
            calls.add(route.request().method() + " " + target.getRawPath()
                    + (target.getRawQuery() == null ? "" : "?" + target.getRawQuery()));
            boolean busy = busyResponses < 0 || calls.size() <= busyResponses;
            route.fulfill(new Route.FulfillOptions().setStatus(busy ? 503 : 200)
                    .setContentType("text/html; charset=UTF-8").setBody(busy ? BUSY : READY));
        };
        page.route(pattern, responder);
    }

    URI uri(String path) { return origin.resolve(path); }
    List<String> calls() { return List.copyOf(calls); }

    @Override public void close() { if (!page.isClosed()) page.unroute(pattern, responder); }
}
