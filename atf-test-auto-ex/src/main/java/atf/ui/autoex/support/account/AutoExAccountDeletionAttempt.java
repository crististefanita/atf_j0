package atf.ui.autoex.support.account;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static atf.api.core.http.request.HttpMethod.GET;
import static atf.api.ui.network.UiNetworkCapturePolicy.ResourceKind.DOCUMENT;
import static atf.api.ui.network.UiNetworkCapturePolicy.ResourceKind.fromPlaywright;

/** Scoped, passive guard: any request to this store's deletion endpoint prevents navigation replay. */
public final class AutoExAccountDeletionAttempt implements AutoCloseable {
    private final Page page;
    private final URI endpoint;
    private final Consumer<Request> listener = this::observe;
    private final List<ObservedRequest> requests = new ArrayList<>();
    private int startedRequests;

    public AutoExAccountDeletionAttempt(Page page, URI endpoint) {
        this.page = Objects.requireNonNull(page, "page");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        page.onRequest(listener);
    }

    private void observe(Request request) {
        URI destination = URI.create(request.url());
        if (endpoint.getScheme().equalsIgnoreCase(destination.getScheme())
                && endpoint.getRawAuthority().equalsIgnoreCase(destination.getRawAuthority())
                && endpoint.getRawPath().equals(destination.getRawPath())) {
            // A diagnostic failure must never authorize replay of an already observed request.
            startedRequests++;
            Request previous = request.redirectedFrom();
            requests.add(new ObservedRequest(request.method(), request.resourceType(),
                    request.isNavigationRequest(), previous == null ? null : previous.url()));
        }
    }

    public int startedRequests() { return startedRequests; }

    public Traffic traffic() { return new Traffic(startedRequests, requests); }

    public AutoExAccountDeletionProgress progressAfter(boolean vignetteDismissed) {
        return new AutoExAccountDeletionProgress(vignetteDismissed, startedRequests(),
                endpoint.toString().equals(page.url()));
    }

    @Override
    public void close() { page.offRequest(listener); }

    /** Browser request events are not equivalent to user document navigations or server mutations. */
    public record Traffic(int observedRequests, List<ObservedRequest> requests) {
        public Traffic {
            requests = List.copyOf(requests);
            if (observedRequests < requests.size()) {
                throw new IllegalArgumentException("observedRequests must include every diagnostic request");
            }
        }

        public long navigationCount() {
            return requests.stream().filter(ObservedRequest::navigationRequest).count();
        }

        public boolean singleDeletionNavigation() {
            var navigations = requests.stream().filter(ObservedRequest::navigationRequest).toList();
            return requests.size() == observedRequests && navigations.size() == 1
                    && GET.value().equals(navigations.getFirst().method())
                    && fromPlaywright(navigations.getFirst().resourceType()) == DOCUMENT;
        }

        public String description() {
            var summary = new StringJoiner(" | ");
            for (int index = 0; index < requests.size(); index++) {
                summary.add("#%d %s".formatted(index + 1, requests.get(index).description()));
            }
            return "%d navigation(s); %d browser request event(s); %s"
                    .formatted(navigationCount(), observedRequests,
                            requests.isEmpty() ? "none" : summary.toString());
        }
    }

    /** Diagnostic facts only: no headers, body, query parameters or redirect credentials. */
    public record ObservedRequest(String method, String resourceType, boolean navigationRequest,
                                  String redirectSourcePath) {
        public ObservedRequest {
            method = Objects.requireNonNull(method, "method");
            resourceType = Objects.requireNonNull(resourceType, "resourceType");
            redirectSourcePath = redirectSourcePath == null ? "none"
                    : Objects.toString(URI.create(redirectSourcePath).getRawPath(), "none");
        }

        public String description() {
            return "%s; resource=%s; navigation=%s; redirectedFrom=%s"
                    .formatted(method, resourceType, navigationRequest, redirectSourcePath);
        }
    }
}
