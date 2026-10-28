package atf.ui.playwright.recovery;

import atf.ui.recovery.*;
import com.microsoft.playwright.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Consumer;

/** Passive, body-free metadata capture. Only requests begun after scope acquisition can produce a receipt. */
final class UiRecoveryObserver implements UiRecoveryLoop.Observation, AutoCloseable {
    private static final int MAX_SIGNALS = 64;
    private final Page page;
    private final Frame frame;
    private final UiRecoveryOperation operation;
    private final UiRecoverySignal.Stamp stamp;
    private final Clock clock;
    private final boolean attached;
    private final IdentityHashMap<Request, Long> owned = new IdentityHashMap<>();
    private final NavigableMap<Long, UiRecoverySignal> signals = new TreeMap<>();
    private final Consumer<Request> requests = this::request;
    private final Consumer<Response> responses = this::response;
    private final Consumer<Request> failures = this::failed;
    private final Consumer<Frame> documents;
    private long sequence;
    private String requestIdentity;
    private boolean documentChanged;
    private boolean conflicting;
    private boolean closed;
    private Throwable captureFailure;

    UiRecoveryObserver(Page page, UiRecoveryOperation operation, UiRecoverySignal.Stamp stamp, Clock clock, boolean attach) {
        this.page = page; this.frame = page.mainFrame(); this.operation = operation; this.stamp = stamp; this.clock = clock; attached = attach;
        documents = changed -> { if (changed == frame) documentChanged = true; };
        if (attach) {
            try { page.onRequest(requests); page.onResponse(responses); page.onRequestFailed(failures); page.onFrameNavigated(documents); }
            catch (RuntimeException | Error primary) { try { close(); } catch (RuntimeException | Error secondary) { primary.addSuppressed(secondary); } throw primary; }
        }
    }
    @Override public boolean currentDocument() { return !closed && !page.isClosed() && !documentChanged && page.mainFrame() == frame; }
    @Override public boolean conflictingRequest() { return conflicting; }
    @Override public Optional<Throwable> failure() { return Optional.ofNullable(captureFailure); }
    @Override public Optional<UiRecoverySignal> signal() { return signals.isEmpty() ? Optional.empty() : Optional.of(signals.lastEntry().getValue()); }

    private void request(Request request) {
        if (closed || documentChanged || !Set.of("document", "xhr", "fetch").contains(request.resourceType())) return;
        try {
            if (request.frame() != frame) return;
            var actual = java.net.URI.create(request.url());
            String origin = UiRecoveryOperation.origin(actual);
            if (!origin.equals(UiRecoveryOperation.origin(operation.endpoint())) && !origin.equals(UiRecoveryOperation.origin(operation.pageOrigin()))) return;
            if (!operation.matches(request.method(), request.url())) { conflicting = true; return; }
            // Full private URL identity is compared only in memory; a redacted signature cannot bind another query.
            if (requestIdentity != null && !requestIdentity.equals(request.url())) { conflicting = true; return; }
            requestIdentity = request.url();
            if (owned.size() >= MAX_SIGNALS) { conflicting = true; return; }
            long next = ++sequence; owned.put(request, next);
            signals.put(next, new UiRecoverySignal(stamp, next, clock.instant(), UiRecoverySignal.Kind.PENDING, 0, ""));
        } catch (RuntimeException unavailable) { captureFailed(unavailable); }
    }
    private void response(Response response) {
        if (closed) return;
        Long index = owned.get(response.request());
        if (index == null) return;
        try {
            String retryAfter = response.headerValue("Retry-After");
            signals.put(index, new UiRecoverySignal(stamp, index, clock.instant(), UiRecoverySignal.Kind.RESPONSE, response.status(), retryAfter));
        } catch (RuntimeException unavailable) { captureFailed(unavailable); }
    }
    private void failed(Request request) {
        if (closed) return;
        Long index = owned.get(request);
        if (index != null) signals.put(index, new UiRecoverySignal(stamp, index, clock.instant(), UiRecoverySignal.Kind.REQUEST_FAILED, 0, ""));
    }
    private void captureFailed(RuntimeException failure) {
        conflicting = true;
        if (captureFailure == null) captureFailure = failure;
        else if (failure != captureFailure) captureFailure.addSuppressed(failure);
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        if (attached && !page.isClosed()) {
            Throwable failure = null;
            for (Runnable cleanup : List.<Runnable>of(() -> page.offRequest(requests), () -> page.offResponse(responses),
                    () -> page.offRequestFailed(failures), () -> page.offFrameNavigated(documents))) {
                try { cleanup.run(); } catch (RuntimeException | Error secondary) { if (failure == null) failure = secondary; else failure.addSuppressed(secondary); }
            }
            owned.clear(); signals.clear(); requestIdentity = null;
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
        }
        owned.clear(); signals.clear();
    }
}
