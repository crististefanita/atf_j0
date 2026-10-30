package atf.autoex.runtime.invoice;

import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.URI;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Only a queue-full response to the exact invoice GET permits a bounded read retry; the click is never replayed. */
public final class AutoExInvoiceDownload {
    private final Page page;
    private final PlaywrightActionSequence actions;
    private final URI origin;
    private final UiRetryPolicy policy;
    private Receipt receipt;

    public AutoExInvoiceDownload(Page page, PlaywrightActionSequence actions, URI origin, UiRetryPolicy policy) {
        this.page = Objects.requireNonNull(page); this.actions = Objects.requireNonNull(actions);
        this.origin = Objects.requireNonNull(origin); this.policy = Objects.requireNonNull(policy);
    }

    public Download download(AtfLocator element, String expected) {
        URI target = URI.create(page.url()).resolve(Objects.requireNonNull(element.locator().getAttribute("href"), "invoice href"));
        if (!sameOrigin(origin, target) || !target.getPath().matches(AutoExRoutes.INVOICE_DOWNLOAD.path() + "[0-9]+")
                || target.getRawQuery() != null || target.getRawFragment() != null)
            throw new IllegalStateException("Invoice target is not an owned read-only download route");
        receipt = null;
        Consumer<Response> listener = response -> {
            var request = response.request();
            if (request.isNavigationRequest() && request.frame().equals(page.mainFrame()))
                receipt = new Receipt(request.method(), URI.create(response.url()), response.status());
        };
        Throwable primary = null;
        try {
            page.onResponse(listener);
            return actions.download(page, element, expected, (budget, downloaded) -> await(target, budget, downloaded));
        } catch (RuntimeException | Error failure) { primary = failure; throw failure; }
        finally {
            try { page.offResponse(listener); }
            catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (primary != cleanup) primary.addSuppressed(cleanup);
            }
        }
    }

    private void await(URI target, PlaywrightActionSequence.ObservationBudget budget, BooleanSupplier downloaded) {
        int attempt = 1;
        while (!downloaded.getAsBoolean()) {
            try {
                page.waitForCondition(() -> downloaded.getAsBoolean() || busyDocument(),
                        new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
            } catch (TimeoutError timeout) {
                throw new IllegalStateException("Invoice download event unavailable; current path=" + URI.create(page.url()).getPath()
                        + "; document=" + (receipt == null ? "unobserved" : receipt.method() + "/" + receipt.status()), timeout);
            }
            if (downloaded.getAsBoolean()) return;
            if (!"GET".equals(receipt.method()) || !target.equals(receipt.uri()))
                throw new IllegalStateException("Invoice recovery refused an unowned document; method=" + receipt.method()
                        + "; path=" + receipt.uri().getPath());
            if (!policy.allows(atf.ui.config.UiRetryOperation.NAVIGATE.label(), attempt))
                throw new IllegalStateException("Invoice GET remains queue full; attempts=" + attempt + "/" + policy.maxAttempts());
            long delay = policy.backoff().toMillis();
            if (delay >= budget.remainingMilliseconds()) throw new IllegalStateException("Invoice GET recovery cannot fit in the original deadline");
            policy.pause();
            budget.remainingMilliseconds();
            actions.information("Invoice GET recovery", target.getPath(), "retry only the observed queue-full invoice GET",
                    "GET; status=" + receipt.status() + "; attempt=" + (++attempt) + "/" + policy.maxAttempts());
            receipt = null;
            try {
                page.navigate(target.toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
                        .setTimeout(budget.remainingMilliseconds()));
            } catch (PlaywrightException navigation) {
                // Chromium aborts document navigation when the response becomes a download; only the actual event can confirm success.
                if (!downloaded.getAsBoolean() && !navigation.getMessage().contains("net::ERR_ABORTED")) throw navigation;
            }
        }
    }

    private boolean busyDocument() {
        if (receipt == null || !receipt.uri().toString().equals(page.url())) return false;
        var busy = page.locator(AutoExSelectors.STORE_BUSY_MESSAGE_LOCATOR);
        return busy.count() == 1 && busy.isVisible();
    }
    private static boolean sameOrigin(URI first, URI second) {
        return Objects.equals(first.getScheme(), second.getScheme()) && Objects.equals(first.getHost(), second.getHost())
                && port(first) == port(second);
    }
    private static int port(URI uri) { return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80; }
    private record Receipt(String method, URI uri, int status) { }
}
