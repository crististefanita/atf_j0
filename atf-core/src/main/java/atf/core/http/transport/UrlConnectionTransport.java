package atf.core.http.transport;

import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Blocking fallback with an owned body subscription, worker and shared request deadline. */
public final class UrlConnectionTransport implements ApiTransport {
    private final ApiAuthenticator authenticator;
    private final Duration timeout;

    public UrlConnectionTransport(ApiAuthenticator authenticator, Duration timeout) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        timeout.toNanos();
    }

    @Override public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String authProfile) {
        return new UrlConnectionOperation(authenticator, timeout, Objects.requireNonNull(request, "request"), url, authProfile).start();
    }
}
