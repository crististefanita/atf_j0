package atf.core.testing.http;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reusable transport decorator that exposes the last semantic request for
 * evidence publication without changing the configured transport.
 */
public final class RecordingApiTransport implements ApiTransport {
    private final ApiTransport delegate;
    private final AtomicReference<ApiRequest> lastRequest = new AtomicReference<>();

    public RecordingApiTransport(ApiTransport delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String authProfile) {
        lastRequest.set(Objects.requireNonNull(request, "request"));
        return delegate.send(request, url, authProfile);
    }

    public ApiRequest lastRequest() {
        return lastRequest.get();
    }
}
