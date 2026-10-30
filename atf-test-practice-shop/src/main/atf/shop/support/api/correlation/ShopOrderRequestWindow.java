package atf.shop.support.api.correlation;

import atf.shop.support.api.ShopApiNetworkJournal.Exchange;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.identity.UiRequestIdentity;
import atf.ui.network.identity.UiRequestTimeline;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Start-based order ownership; receipt order remains available for raw trace and sibling checkpoints. */
public final class ShopOrderRequestWindow {
    private final UiRequestTimeline pageRequests = new UiRequestTimeline();
    private final Map<Exchange, UiRequestIdentity> starts = new IdentityHashMap<>();
    private final Map<Long, Optional<UiRequestIdentity>> legacyMarkers = new HashMap<>();
    private Supplier<UiRequestIdentity> markers = pageRequests::marker;

    public void attach(Page page) { pageRequests.attach(page); }
    public void attach(UiNetworkInspector inspector) { markers = inspector::requestMarker; }
    public UiRequestIdentity marker() { return markers.get(); }
    /** Sample the receipt only after the browser barrier; numeric collisions cannot redefine an earlier window. */
    public long markLegacy(LongSupplier receipts) {
        var fence = marker();
        long receipt = receipts.getAsLong();
        legacyMarkers.merge(receipt, Optional.of(fence), (previous, current) -> previous.filter(fence::equals));
        return receipt;
    }
    public UiRequestIdentity legacy(long receiptSequence) {
        var captured = legacyMarkers.get(receiptSequence);
        return captured != null ? captured.orElse(null) : receiptSequence == 0 ? marker().beginning() : null;
    }
    public UiRequestIdentity identity(Request request) { return pageRequests.identity(request).orElse(null); }
    public void forget(Request request) { pageRequests.forget(request); }
    public void record(Exchange exchange, UiRequestIdentity identity) { if (identity != null) starts.put(exchange, identity); }
    public boolean follows(Exchange exchange, UiRequestIdentity marker) {
        var start = starts.get(exchange);
        return start != null && start.isAfter(marker);
    }
}
