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
        request.headers().forEach(builder::header);
        authenticator.apply(builder, authProfile);
        long started = System.nanoTime();
        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> new ApiResponse(
                        response.statusCode(), response.body(), ApiResponse.flatten(response.headers()),
                        Duration.ofNanos(System.nanoTime() - started), url));
    }
}
