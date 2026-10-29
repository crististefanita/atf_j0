package atf.shop.support.catalog;

import atf.core.http.execution.RetryAfterParser;
import atf.shop.routes.ShopUiRoutes;
import atf.shop.api.ShopApiRoutes;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.recovery.UiRecoveryOperation;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

/** Observes one initial catalog read; a transient response permits one home-document GET, never an action replay. */
public final class ShopCatalogReadiness implements AutoCloseable {
    private static final int MAX_HOME_REFRESHES = 1;
    private static final Set<Integer> TRANSIENT = Set.of(429, 502, 503, 504);
    private static final Set<String> TRANSIENT_FAILURES = Set.of("net::ERR_CONNECTION_RESET", "net::ERR_CONNECTION_CLOSED",
            "net::ERR_NETWORK_CHANGED", "net::ERR_TIMED_OUT");
    private final Page page;
    private final URI home;
    private final URI endpoint;
    private final String homeOrigin;
    private final String catalogOrigin;
    private final PlaywrightActionSequence actions;
    private final Consumer<Request> requested = this::requested;
    private final Consumer<Response> responded = this::responded;
    private final Consumer<Request> failed = this::failed;
    private final Consumer<Frame> navigated = this::navigated;
    private Request current;
    private Receipt receipt;
    private int requests;
    private long documents;
    private long expectedDocument = -1;
    private boolean closed;
    private boolean evaluated;

    public ShopCatalogReadiness(Page page, URI home, URI endpoint, PlaywrightActionSequence actions) {
        this.page = Objects.requireNonNull(page, "page");
        this.home = Objects.requireNonNull(home, "home").normalize();
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint").normalize();
        this.actions = Objects.requireNonNull(actions, "actions");
        homeOrigin = UiRecoveryOperation.origin(this.home);
        catalogOrigin = UiRecoveryOperation.origin(this.endpoint);
        if (!ShopUiRoutes.HOME.equals(home.getPath())
                && !home.getPath().isEmpty()) throw new IllegalArgumentException("Catalog refresh requires the home route");
        if (endpoint.getRawQuery() != null
                || endpoint.getUserInfo() != null || endpoint.getRawFragment() != null
                || !ShopApiRoutes.PRODUCT_COLLECTION.equals(endpoint.getRawPath())) {
            throw new IllegalArgumentException("Catalog endpoint must be the explicit HTTP " + ShopApiRoutes.PRODUCT_COLLECTION + " read URI");
        }
        try {
            page.onRequest(requested);page.onResponse(responded);
            page.onRequestFailed(failed);page.onFrameNavigated(navigated);
        } catch (RuntimeException | Error failure) {
            try {close();} catch (RuntimeException | Error cleanup) {if (cleanup != failure) failure.addSuppressed(cleanup);}
            throw failure;
        }
    }

    private void requested(Request request) {
        URI actual = URI.create(request.url()).normalize();
        if (!closed && endpoint.getRawPath().equals(actual.getRawPath()) && actual.getRawQuery() == null
                && catalogOrigin.equals(UiRecoveryOperation.origin(actual)) && Set.of("GET", "QUERY").contains(request.method())
                && request.frame() == page.mainFrame()) {
            current = request;
            receipt = null;
            requests++;
        }
    }

    private void responded(Response response) {
        if (!closed && response.request() == current) {
            receipt = new Receipt(requests, current.method(), response.status(), response.headerValue("retry-after"), "");
        }
    }

    private void failed(Request request) {
        if (!closed && request == current) receipt = new Receipt(requests, request.method(), 0, "",
                Objects.requireNonNullElse(request.failure(), "unavailable"));
    }

    private void navigated(Frame frame) { if (!closed && frame == page.mainFrame()) documents++; }

    public void await(Locator product, PlaywrightActionSequence.ObservationBudget budget) {
        if (closed || evaluated) throw new IllegalStateException("Catalog observation is single-use and cannot renew its deadline");
        evaluated = true;
        expectedDocument = documents;
        for (int refresh = 0; refresh <= MAX_HOME_REFRESHES; refresh++) {
            requireHome();
            page.waitForCondition(() -> product.isVisible() || receipt != null || !atHome(),
                    new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
            requireHome();
            Receipt observed = receipt;
            if (observed != null) actions.information("Catalog response #" + observed.attempt(), endpoint.toString(),
                    "initial catalog read; original HTTP method is retained",
                    "method=" + observed.method() + "; status=" + observed.statusText() + "; failure=" + observed.failure()
                            + "; homeRefreshes=" + refresh);
            if (observed == null || observed.status() >= 200 && observed.status() < 300) {
                page.waitForCondition(() -> product.isVisible() || !atHome(),
                        new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
                requireHome();
                return;
            }
            if (!(TRANSIENT.contains(observed.status()) || TRANSIENT_FAILURES.contains(observed.failure())) || refresh == MAX_HOME_REFRESHES) {
                throw new IllegalStateException("Catalog read refused: " + observed.method() + " " + endpoint
                        + "; HTTP " + observed.statusText() + "; failure=" + observed.failure() + "; homeRefreshes=" + refresh);
            }
            Duration delay = RetryAfterParser.parse(observed.retryAfter(),
                    observed.retryAfter() == null || observed.retryAfter().isBlank()
                            ? Duration.ZERO : Duration.ofMillis(Long.MAX_VALUE), Clock.systemUTC());
            if (delay.compareTo(Duration.ofMillis((long) budget.remainingMilliseconds())) >= 0) {
                throw new TimeoutError("Catalog Retry-After exceeds the original readiness deadline");
            }
            if (!delay.isZero()) page.waitForTimeout(delay.toNanos() / 1_000_000d);
            requireHome();
            actions.information("Retry catalog home GET", home.toString(),
                    "one refresh after a confirmed transient catalog response; same readiness deadline",
                    "catalogMethod=" + observed.method() + "; status=" + observed.statusText() + "; refresh=" + (refresh + 1) + "/" + MAX_HOME_REFRESHES);
            receipt = null;
            current = null;
            page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(budget.remainingMilliseconds()));
            expectedDocument = documents;
        }
    }

    private void requireHome() {
        if (!atHome()) throw new IllegalStateException("Catalog observation cannot refresh a different document: " + page.url());
    }

    private boolean atHome() {
        if (expectedDocument >= 0 && documents != expectedDocument) return false;
        URI actual = URI.create(page.url()).normalize();
        String expectedPath = home.getPath().isEmpty() ? ShopUiRoutes.HOME : home.getPath();
        String actualPath = actual.getPath().isEmpty() ? ShopUiRoutes.HOME : actual.getPath();
        try {
            return homeOrigin.equals(UiRecoveryOperation.origin(actual)) && expectedPath.equals(actualPath)
                    && Objects.equals(home.getRawQuery(), actual.getRawQuery());
        } catch (IllegalArgumentException invalidOrigin) {return false;}
    }

    public void diagnose(Throwable failure, Locator product) {
        try {
            actions.information("Catalog readiness failed", endpoint.toString(), "visible product within the original deadline",
                    "url=" + page.url() + "; document=" + page.evaluate("()=>document.readyState")
                            + "; products=" + product.count() + "; requests=" + requests + "; response=" + receipt);
        } catch (RuntimeException | Error diagnostic) {
            if (diagnostic != failure) failure.addSuppressed(diagnostic);
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        try { page.offRequest(requested); }
        finally {
            try { page.offResponse(responded); }
            finally { try { page.offRequestFailed(failed); } finally { page.offFrameNavigated(navigated); } }
        }
    }

    private record Receipt(int attempt, String method, int status, String retryAfter, String failure) {
        String statusText() {return status == 0 ? "unavailable" : Integer.toString(status);}
        @Override public String toString() {
            return "Receipt[attempt=" + attempt + ", method=" + method + ", status=" + statusText()
                    + ", retryAfter=" + retryAfter + ", failure=" + failure + "]";
        }
    }
}
