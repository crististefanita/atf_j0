package atf.autoex.runtime.navigation;

import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import java.net.URI;
import java.util.Objects;

import static atf.core.http.request.HttpMethod.GET;
import static atf.ui.config.UiRetryOperation.NAVIGATE;

/** Recovers only the site's explicit queue-full document on observed, allowlisted GET navigation. */
public final class AutoExReadOnlyNavigation {
    private final Page page;
    private final PlaywrightActionSequence actions;
    private final URI baseUrl;
    private final UiRetryPolicy policy;
    private Document document;

    public AutoExReadOnlyNavigation(Page page, PlaywrightActionSequence actions, URI baseUrl, UiRetryPolicy policy) {
        this.page = Objects.requireNonNull(page, "page");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.policy = Objects.requireNonNull(policy, "policy");
        page.onResponse(this::observeDocument);
    }

    public void recoverIfBusy() {
        int attempt = 1;
        while (isQueueFull()) {
            URI current = URI.create(page.url());
            boolean safe = isReadOnlyDocument(current);
            String method = document == null ? "UNKNOWN" : document.method();
            actions.observation("Store availability", current.getPath(), "store renders its business page",
                    "queue full; document attempt " + attempt + "/" + policy.maxAttempts() + "; " + method);
            if (!safe || !policy.allows(NAVIGATE.label(), attempt)) {
                throw new AutoExStoreUnavailableException(method, current.getPath(), attempt, policy.maxAttempts(), safe);
            }
            policy.pause();
            actions.navigate(page, current, "read-only store document recovers after queue full");
            attempt++;
        }
    }

    private boolean isQueueFull() {
        var message = page.locator(AutoExSelectors.STORE_BUSY_MESSAGE_LOCATOR);
        return message.count() == 1 && message.isVisible();
    }

    private void observeDocument(Response response) {
        var request = response.request();
        if (request.isNavigationRequest() && request.frame().equals(page.mainFrame())) {
            document = new Document(request.method(), URI.create(response.url()));
        }
    }

    private boolean isReadOnlyDocument(URI current) {
        if (document == null || !GET.value().equals(document.method()) || !sameOrigin(baseUrl, current)
                || !sameOrigin(document.uri(), current)
                || !Objects.equals(document.uri().getRawPath(), current.getRawPath())
                || !Objects.equals(document.uri().getRawQuery(), current.getRawQuery())) return false;
        String path = current.getPath();
        return AutoExRoutes.HOME.path().equals(path) || AutoExRoutes.PRODUCTS.path().equals(path)
                || path.startsWith(AutoExRoutes.PRODUCT_DETAILS.path())
                && path.substring(AutoExRoutes.PRODUCT_DETAILS.path().length()).matches("[A-Za-z0-9_-]+");
    }

    private static boolean sameOrigin(URI first, URI second) {
        return Objects.equals(first.getScheme(), second.getScheme())
                && Objects.equals(first.getHost(), second.getHost()) && port(first) == port(second);
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
    }

    private record Document(String method, URI uri) { }
}
