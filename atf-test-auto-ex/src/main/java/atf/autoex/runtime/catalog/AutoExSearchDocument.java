package atf.autoex.runtime.catalog;

import atf.autoex.support.catalog.AutoExCatalogAssertions;
import atf.ui.playwright.PlaywrightActionSequence.ObservationBudget;
import atf.ui.playwright.lifecycle.UiListenerScope;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import java.net.URI;
import java.util.function.Consumer;

/** One passive search-document scope; stale responses and a second navigation cannot certify its click. */
final class AutoExSearchDocument implements AutoCloseable {
    private final Page page;
    private final UiListenerScope listeners = new UiListenerScope();
    private final Consumer<Request> requests = this::requested;
    private final Consumer<Response> responses = this::responded;
    private final Consumer<Request> failures = this::failed;
    private final double previousOrigin;
    private Request request;
    private String method = "UNKNOWN", url = "<unobserved>", failure;
    private int status, count;

    AutoExSearchDocument(Page page) {
        this.page = page;
        previousOrigin = ((Number) page.evaluate("performance.timeOrigin")).doubleValue();
        listeners.add(() -> page.onRequest(requests), () -> page.offRequest(requests))
            .add(() -> page.onResponse(responses), () -> page.offResponse(responses))
            .add(() -> page.onRequestFailed(failures), () -> page.offRequestFailed(failures));
        listeners.attach();
    }
    private void requested(Request next) {
        if (!listeners.active() || !next.isNavigationRequest() || next.frame() != page.mainFrame()) return;
        count++;
        if (request == null) { request = next; method = next.method(); url = next.url(); }
    }
    private void responded(Response response) {
        if (listeners.active() && response.request() == request) status = response.status();
    }
    private void failed(Request failed) {
        if (listeners.active() && failed == request) failure = failed.failure();
    }
    void awaitResponse(ObservationBudget budget) {
        page.waitForCondition(() -> status != 0 || failure != null || count > 1,
            new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
        if (failure != null) throw new IllegalStateException("Search transport failed: " + failure + "; " + description());
        requireSingleRequest();
    }
    void requireSearch(URI products, String query) {
        requireSingleRequest();
        if (!"GET".equals(method) || !AutoExCatalogAssertions.matchesSearchLocation(products, query, url))
            throw new IllegalStateException("Search document does not match the owned GET route; " + description());
        if (status < 200 || status >= 300) throw new IllegalStateException("Search document rejected: HTTP " + status + "; " + description());
    }
    double previousOrigin() { return previousOrigin; }
    void requireSingleRequest() {
        if (count != 1) throw new IllegalStateException("Search requires one document request, without replay; " + description());
    }
    String description() {
        return "document=" + method + "; HTTP " + (status == 0 ? "unobserved" : status)
                + "; request URL=" + url + "; navigation requests=" + count;
    }
    String url() { return url; }
    @Override public void close() { try { listeners.close(); } finally { request = null; } }
}
