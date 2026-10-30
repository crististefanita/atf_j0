package atf.shop.tests.support.causality;

import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkInspector;
import com.microsoft.playwright.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/** Public Page/inspector seams deliver a queued response during the marker's passive round trip. */
public final class ShopLegacyMarkerFixture implements AutoCloseable {
    private static final String URL = "https://api.practicesoftwaretesting.com/invoices/guest";
    private final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();
    private final boolean generic;
    private final UiNetworkInspector inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(
            List.of("api.practicesoftwaretesting.com")));
    private final ShopApiNetworkJournal journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
            ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
    private Runnable queued;
    private int activations;

    public ShopLegacyMarkerFixture(boolean generic) {
        this.generic = generic;
        var page = proxy(Page.class, (method, args) -> {
            if (method.equals("title")) {
                Runnable pending = queued; queued = null;
                if (pending != null) pending.run();
                return "Controlled marker document";
            }
            if (method.startsWith("on") || method.startsWith("off")) {
                @SuppressWarnings("unchecked") var listener = (Consumer<Object>) args[0];
                var subscribers = listeners.computeIfAbsent(method.substring(method.startsWith("off") ? 3 : 2), ignored -> new ArrayList<>());
                if (method.startsWith("off")) subscribers.remove(listener); else subscribers.add(listener);
                return null;
            }
            throw new AssertionError("Unexpected page capability: " + method);
        });
        if (generic) { journal.attach(inspector); inspector.attach(page); } else journal.attach(page);
    }
    public ShopApiNetworkJournal journal() { return journal; }
    public int activations() { return activations; }
    public void pendingResponse() { queued = () -> complete(begin(), 201, "old-order"); }
    public Request begin() {
        var request = proxy(Request.class, (method, args) -> switch (method) {
            case "url" -> URL;
            case "method" -> "POST";
            case "resourceType" -> "fetch";
            case "postData" -> "{}";
            case "headers", "allHeaders" -> Map.of();
            default -> throw new AssertionError("Unexpected request capability: " + method);
        });
        listeners.getOrDefault("Request", List.of()).forEach(listener -> listener.accept(request));
        return request;
    }
    public void confirm(int status) { activations++; complete(begin(), status, "current-order"); }
    public void complete(Request request, int status, String id) {
        var response = proxy(Response.class, (method, args) -> switch (method) {
            case "request" -> request;
            case "status" -> status;
            case "headers" -> Map.of("content-type", "application/json");
            case "finished" -> null;
            case "body" -> ("{\"id\":\"" + id + "\",\"invoice_number\":\"INV-12\",\"total\":24.02}").getBytes(StandardCharsets.UTF_8);
            default -> throw new AssertionError("Unexpected response capability: " + method);
        });
        if (generic) inspector.capture(response);
        else listeners.getOrDefault("Response", List.of()).forEach(listener -> listener.accept(response));
    }
    @Override public void close() { inspector.close(); }
    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> port) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) ->
                switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> instance == args[0];
                    case "toString" -> type.getSimpleName() + " controlled port";
                    default -> port.apply(method.getName(), args);
                }));
    }
}
