package atf.shop.support.cart;

import atf.shop.api.ShopApiRoutes;
import atf.shop.support.ShopJsonEvidence;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.*;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

/** Causal acknowledgement of one product mutation; a transient toast is never the source of success. */
public final class ShopCartAdditionConfirmation {
    private final Page page;
    private final URI api;
    private final String product;
    private final int quantity;
    private final Consumer<Request> started = this::started;
    private final Consumer<Response> responded = this::responded;
    private final Consumer<Request> failed = this::failed;
    private Request owned;
    private Integer status;
    private String networkFailure;
    private int matches;
    private boolean requestHook, responseHook, failureHook, closed;

    public ShopCartAdditionConfirmation(Page page, URI api, String product, int quantity) {
        this.page = Objects.requireNonNull(page); this.api = Objects.requireNonNull(api);
        this.product = Objects.requireNonNull(product); this.quantity = quantity;
        if (product.isBlank() || quantity < 1) throw new IllegalArgumentException("A cart addition requires product identity and positive quantity");
    }

    public void activate(PlaywrightActionSequence actions, AtfLocator button) {
        Throwable primary = null;
        try {
            page.title(); // Drain queued older request starts before installing this activation's fence.
            requestHook = true; page.onRequest(started);
            responseHook = true; page.onResponse(responded);
            failureHook = true; page.onRequestFailed(failed);
            actions.clickAndAwait(button, "configured product addition is acknowledged by its own response", budget -> {
                try {
                    page.waitForCondition(() -> status != null || networkFailure != null || matches > 1,
                            new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
                } catch (TimeoutError timeout) {
                    throw new IllegalStateException("Cart addition response was not observed; matching request starts=" + matches, timeout);
                }
                if (matches != 1 || networkFailure != null || status == null || status < 200 || status >= 300)
                    throw new IllegalStateException("Cart addition was not accepted; matching starts=" + matches
                            + "; status=" + status + "; network=" + networkFailure);
            });
            actions.information("Cart addition acknowledgement", URI.create(owned.url()).getPath(),
                    "the owned product/quantity request is accepted before reading the cart",
                    "POST; status=" + status + "; product=" + product + "; quantity=" + quantity + "; toast is diagnostic only");
        } catch (RuntimeException | Error failure) { primary = failure; throw failure; }
        finally {
            Throwable ending = release(primary);
            if (primary == null && ending instanceof RuntimeException runtime) throw runtime;
            if (primary == null && ending instanceof Error error) throw error;
        }
    }

    private void started(Request request) {
        if (closed || !"POST".equals(request.method())) return;
        URI uri = URI.create(request.url());
        if (!sameOrigin(api, uri) || !uri.getPath().matches(ShopApiRoutes.CART_RESOURCE_PREFIX + "[^/]+")
                || uri.getRawQuery() != null) return;
        var body = ShopJsonEvidence.parse(request.postData());
        if (body == null || !product.equals(body.path("product_id").asText())
                || !body.path("quantity").isIntegralNumber() || body.path("quantity").intValue() != quantity) return;
        matches++;
        if (owned == null) owned = request;
    }
    private void responded(Response response) { if (!closed && owned != null && response.request() == owned) status = response.status(); }
    private void failed(Request request) { if (!closed && owned != null && request == owned) networkFailure = Objects.toString(request.failure(), "request failed"); }

    private Throwable release(Throwable primary) {
        closed = true;
        if (requestHook) primary = remove(primary, () -> page.offRequest(started));
        if (responseHook) primary = remove(primary, () -> page.offResponse(responded));
        if (failureHook) primary = remove(primary, () -> page.offRequestFailed(failed));
        return primary;
    }
    private static Throwable remove(Throwable primary, Runnable remove) {
        try { remove.run(); }
        catch (RuntimeException | Error failure) {
            if (primary == null) return failure;
            if (primary != failure) primary.addSuppressed(failure);
        }
        return primary;
    }
    private static boolean sameOrigin(URI first, URI second) {
        return Objects.equals(first.getScheme(), second.getScheme()) && Objects.equals(first.getHost(), second.getHost())
                && port(first) == port(second);
    }
    private static int port(URI uri) { return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80; }
}
