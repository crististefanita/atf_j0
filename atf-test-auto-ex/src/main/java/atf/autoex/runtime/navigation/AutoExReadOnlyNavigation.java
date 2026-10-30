package atf.autoex.runtime.navigation;

import atf.ui.config.UiRetryPolicy;
import atf.ui.recovery.UiRecoveryOperation;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.URI;
import java.util.Objects;

import static atf.core.http.request.HttpMethod.GET;
import static atf.ui.config.UiRetryOperation.NAVIGATE;

/** Recovers explicit queue-full or observed HTTP 520/525 documents only on observed, allowlisted GET navigation. */
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
        recover(1, null);
    }

    /** One recovery scope retains its attempt count and the enclosing action's deadline. */
    public Recovery within(PlaywrightActionSequence.ObservationBudget budget) {
        return new Recovery(Objects.requireNonNull(budget));
    }

    public final class Recovery {
        private final PlaywrightActionSequence.ObservationBudget budget;
        private int attempt = 1;
        private Recovery(PlaywrightActionSequence.ObservationBudget budget) { this.budget = budget; }
        public void recoverIfBusy() { attempt = recover(attempt, budget); }
    }

    private int recover(int attempt, PlaywrightActionSequence.ObservationBudget budget) {
        String reason;
        while ((reason = transientReason()) != null) {
            URI current = URI.create(page.url());
            boolean safe = isReadOnlyDocument(current);
            String method = document == null ? "UNKNOWN" : document.method();
            actions.information("Store availability", current.getPath(), "store renders its business page",
                    reason + "; document attempt " + attempt + "/" + policy.maxAttempts() + "; " + method);
            if (!safe || !policy.allows(NAVIGATE.label(), attempt)) {
                throw new AutoExStoreUnavailableException(reason, method, current.getPath(), attempt, policy.maxAttempts(), safe);
            }
            if (budget != null && policy.backoff().toMillis() >= budget.remainingMilliseconds())
                throw new IllegalStateException("Store GET recovery cannot fit in the original action deadline");
            policy.pause();
            if (budget == null) actions.navigate(page, current, "read-only store document recovers after " + reason);
            else {
                actions.information("Store GET recovery", current.getPath(), "retry only the observed transient read-only document",
                        "GET; " + reason + "; attempt=" + (attempt + 1) + "/" + policy.maxAttempts());
                page.navigate(current.toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT)
                        .setTimeout(budget.remainingMilliseconds()));
            }
            attempt++;
        }
        return attempt;
    }

    private String transientReason() {
        if (document != null && document.uri().toString().equals(page.url()) && (document.status() == 520 || document.status() == 525)) return "HTTP " + document.status();
        return isQueueFull() ? "queue full" : null;
    }

    public void requireProductDocument(URI expected) {
        if (document == null || !"GET".equals(document.method()) || !expected.equals(document.uri())
                || !expected.toString().equals(page.url()) || document.status() < 200 || document.status() >= 300)
            throw new IllegalStateException("Product document is not a confirmed successful owned GET; " + description());
    }

    public String description() {
        return document == null ? "document=unobserved" : "document=" + document.method() + "/" + document.status()
                + "; observed path=" + document.uri().getPath();
    }

    private boolean isQueueFull() {
        var message = page.locator(AutoExSelectors.STORE_BUSY_MESSAGE_LOCATOR);
        return message.count() == 1 && message.isVisible();
    }

    private void observeDocument(Response response) {
        var request = response.request();
        if (request.isNavigationRequest() && request.frame().equals(page.mainFrame())) {
            document = new Document(request.method(), URI.create(response.url()), response.status());
        }
    }

    private boolean isReadOnlyDocument(URI current) {
        if (document == null || !GET.value().equals(document.method()) || !UiRecoveryOperation.origin(baseUrl).equals(UiRecoveryOperation.origin(current))
                || !UiRecoveryOperation.origin(document.uri()).equals(UiRecoveryOperation.origin(current))
                || !Objects.equals(document.uri().getRawPath(), current.getRawPath())
                || !Objects.equals(document.uri().getRawQuery(), current.getRawQuery())) return false;
        String path = current.getPath();
        return AutoExRoutes.HOME.path().equals(path) || AutoExRoutes.PRODUCTS.path().equals(path)
                || path.startsWith(AutoExRoutes.PRODUCT_DETAILS.path())
                && path.substring(AutoExRoutes.PRODUCT_DETAILS.path().length()).matches("[A-Za-z0-9_-]+");
    }

    private record Document(String method, URI uri, int status) { }
}
