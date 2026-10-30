package atf.ui.network.identity;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

/** Owned browser request-start ledger, reusable by raw adapters and the network inspector. */
public final class UiRequestTimeline implements AutoCloseable {
    private final UUID scope = UUID.randomUUID();
    private final Map<Request, UiRequestIdentity> active = new IdentityHashMap<>();
    private final Consumer<Request> listener = this::observe;
    private long started;
    private Page page;
    private boolean closed;

    public synchronized void attach(Page owner) {
        if (page != null || closed) throw new IllegalStateException("Request timeline is already attached or closed");
        page = java.util.Objects.requireNonNull(owner, "page");
        try { page.onRequest(listener); }
        catch (RuntimeException | Error failure) {
            try { close(); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    /** Explicit start seam for an adapter that already observed the request beginning. */
    public synchronized UiRequestIdentity begin() {
        if (closed) throw new IllegalStateException("Request timeline is closed");
        return new UiRequestIdentity(scope, ++started);
    }
    public synchronized UiRequestIdentity marker() {
        // A passive browser round trip drains earlier queued request-start events before the action fence.
        if (page != null) page.title();
        return new UiRequestIdentity(scope, started);
    }
    public synchronized Optional<UiRequestIdentity> identity(Request request) { return Optional.ofNullable(active.get(request)); }
    public synchronized void forget(Request request) { active.remove(request); }
    private synchronized void observe(Request request) { if (!closed && request != null) active.put(request, begin()); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        try { if (page != null) page.offRequest(listener); }
        finally { page = null; active.clear(); }
    }
}
