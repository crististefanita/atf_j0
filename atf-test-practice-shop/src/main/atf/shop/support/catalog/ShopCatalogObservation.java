package atf.shop.support.catalog;

import atf.ui.playwright.lifecycle.UiListenerScope;
import atf.ui.recovery.UiRecoveryOperation;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Passive latest-request observation; eligibility is Shop's GET/QUERY protocol, never a refresh decision. */
final class ShopCatalogObservation implements AutoCloseable {
    private final Page page;
    private final URI endpoint;
    private final String origin;
    private final UiListenerScope listeners = new UiListenerScope();
    private final Consumer<Request> requested = this::requested;
    private final Consumer<Response> responded = this::responded;
    private final Consumer<Request> failed = this::failed;
    private final Consumer<Frame> navigated = this::navigated;
    private Request current;
    private Receipt receipt;
    private int requests;
    private long documents;

    ShopCatalogObservation(Page page, URI endpoint) {
        this.page=page; this.endpoint=endpoint; origin=UiRecoveryOperation.origin(endpoint);
        listeners.add(() -> page.onRequest(requested), () -> page.offRequest(requested))
                .add(() -> page.onResponse(responded), () -> page.offResponse(responded))
                .add(() -> page.onRequestFailed(failed), () -> page.offRequestFailed(failed))
                .add(() -> page.onFrameNavigated(navigated), () -> page.offFrameNavigated(navigated));
        listeners.attach();
    }
    private void requested(Request request) {
        if (!listeners.active()) return;
        URI actual = URI.create(request.url()).normalize();
        if (endpoint.getRawPath().equals(actual.getRawPath()) && actual.getRawQuery() == null
                && origin.equals(UiRecoveryOperation.origin(actual)) && Set.of("GET", "QUERY").contains(request.method())
                && request.frame() == page.mainFrame()) {
            current=request; receipt=null; requests++;
        }
    }
    private void responded(Response response) {
        if (listeners.active() && response.request() == current)
            receipt=new Receipt(requests, current.method(), response.status(), response.headerValue("retry-after"), "");
    }
    private void failed(Request request) {
        if (listeners.active() && request == current)
            receipt=new Receipt(requests, request.method(), 0, "", Objects.requireNonNullElse(request.failure(), "unavailable"));
    }
    private void navigated(Frame frame) { if (listeners.active() && frame == page.mainFrame()) documents++; }
    Receipt latest() { return receipt; }
    int requests() { return requests; }
    long documents() { return documents; }
    void resetRequest() { current=null;receipt=null; }
    @Override public void close() { resetRequest(); listeners.close(); }

    record Receipt(int attempt, String method, int status, String retryAfter, String failure) {
        String statusText() { return status == 0 ? "unavailable" : Integer.toString(status); }
        @Override public String toString() {
            return "Receipt[attempt=" + attempt + ", method=" + method + ", status=" + statusText()
                    + ", retryAfter=" + retryAfter + ", failure=" + failure + "]";
        }
    }
}
