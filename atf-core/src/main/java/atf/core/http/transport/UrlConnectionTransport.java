package atf.core.http.transport;
import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

/**
 * Blocking URLConnection transport exposed through the same asynchronous ATF
 * transport contract as {@link HttpClientTransport}. It is useful for live
 * smoke runs on JDK environments where the java.net.http selector cannot
 * create its internal loopback channel.
 */
public final class UrlConnectionTransport implements ApiTransport {
    private final ApiAuthenticator authenticator;
    private final Duration timeout;

    public UrlConnectionTransport(ApiAuthenticator authenticator, Duration timeout) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String authProfile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(request, url, authProfile);
            } catch (IOException | InterruptedException error) {
                if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                throw new CompletionException(error);
            }
        });
    }

    private ApiResponse execute(ApiRequest request, String url, String authProfile)
            throws IOException, InterruptedException {
        long started = System.nanoTime();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            int timeoutMillis = Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(1, timeout.toMillis())));
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod(request.method());
            connection.setDoInput(true);
            HttpHeaderProfiles.JSON_REQUEST.forEach(connection::setRequestProperty);
            request.headers().forEach(connection::setRequestProperty);
            applyAuthentication(connection, url, authProfile);

            byte[] body = bodyBytes(request.body());
            if (body.length > 0) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                connection.getOutputStream().write(body);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = stream == null ? "" : new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return new ApiResponse(status, responseBody, responseHeaders(connection),
                    Duration.ofNanos(System.nanoTime() - started), url);
        } finally {
            connection.disconnect();
        }
    }

    private void applyAuthentication(HttpURLConnection connection, String url, String authProfile) {
        var builder = HttpRequest.newBuilder(URI.create(url));
        authenticator.apply(builder, authProfile);
        builder.build().headers().map().forEach((name, values) ->
                connection.setRequestProperty(name, String.join(", ", values)));
    }

    private static Map<String, String> responseHeaders(HttpURLConnection connection) {
        var headers = new LinkedHashMap<String, String>();
        connection.getHeaderFields().forEach((name, values) -> {
            if (name != null && values != null) headers.put(name, String.join(", ", values));
        });
        return headers;
    }

    private static byte[] bodyBytes(HttpRequest.BodyPublisher publisher) throws IOException, InterruptedException {
        var result = new CompletableFuture<byte[]>();
        var output = new ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override public void onNext(ByteBuffer buffer) {
                var copy = buffer.slice();
                var bytes = new byte[copy.remaining()];
                copy.get(bytes);
                output.writeBytes(bytes);
            }

            @Override public void onError(Throwable error) { result.completeExceptionally(error); }

            @Override public void onComplete() { result.complete(output.toByteArray()); }
        });
        try {
            return result.get();
        } catch (java.util.concurrent.ExecutionException error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            if (cause instanceof IOException io) throw io;
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IOException("Unable to read request body", cause);
        }
    }
}
