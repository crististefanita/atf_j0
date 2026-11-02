package atf.core.http.transport;
import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class HttpClientTransport implements ApiTransport {
    private final HttpClient client;
    private final ApiAuthenticator authenticator;
    private final Duration timeout;

    public HttpClientTransport(HttpClient client, ApiAuthenticator authenticator, Duration timeout) {
        this.client = client;
        this.authenticator = authenticator;
        this.timeout = timeout;
    }

    @Override
    public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String authProfile) {
        var builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .method(request.method(), request.body());
        HttpHeaderProfiles.JSON_REQUEST.forEach(builder::header);
        request.headers().forEach(builder::setHeader);
        authenticator.apply(builder, authProfile);
        long started = System.nanoTime();
        var upstream = client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        var result = new ResponseFuture(upstream);
        upstream.whenComplete((response, failure) -> {
            if (failure != null) result.completeExceptionally(failure);
            else {
                try {
                    result.complete(new ApiResponse(response.statusCode(), response.body(), ApiResponse.flatten(response.headers()),
                            Duration.ofNanos(System.nanoTime() - started), url));
                } catch (Throwable projectionFailure) { result.completeExceptionally(projectionFailure); }
            }
        });
        return result;
    }
    /** Cancellation is best effort; it neither confirms server rollback nor authorizes replay. */
    private static final class ResponseFuture extends CompletableFuture<ApiResponse> {
        private final CompletableFuture<?> upstream;
        private final java.util.concurrent.atomic.AtomicBoolean forwarded = new java.util.concurrent.atomic.AtomicBoolean();
        private ResponseFuture(CompletableFuture<?> upstream) { this.upstream = upstream; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) {
            boolean accepted = super.cancel(mayInterruptIfRunning);
            if (accepted && forwarded.compareAndSet(false, true)) {
                try { upstream.cancel(mayInterruptIfRunning); }
                catch (RuntimeException cleanupFailure) {
                    handle((value, terminal) -> { terminal.addSuppressed(cleanupFailure); return null; });
                }
            }
            return accepted;
        }
    }
}
