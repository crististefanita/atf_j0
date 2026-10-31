package atf.core.http.transport;

import atf.core.http.request.ApiRequest;
import atf.core.http.auth.AuthProfile;
import atf.core.http.auth.AuthType;
import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class HttpClientRequestHeadersTest {
    @ParameterizedTest
    @ValueSource(strings = {"content-type", "Content-Type", "CONTENT-TYPE"})
    void explicitHeadersReplaceDefaultsCaseInsensitivelyWithoutResending(String contentType) {
        HttpRequest request = record(Map.of(contentType, "application/x-www-form-urlencoded", "aCcEpT", "text/plain"));
        assertEquals(List.of("application/x-www-form-urlencoded"), request.headers().allValues("Content-Type"));
        assertEquals(List.of("text/plain"), request.headers().allValues("Accept"));
    }

    @Test
    void retainsDefaultAcceptAndAuthenticationWithAnExplicitPayloadType() {
        HttpRequest request = record(Map.of("content-type", "text/csv"));
        assertEquals(List.of("text/csv"), request.headers().allValues("Content-Type"));
        assertEquals(List.of("application/json"), request.headers().allValues("Accept"));
        assertEquals(List.of("Bearer UNIT_AUTH_CANARY"), request.headers().allValues("Authorization"));
    }

    @Test
    void theSelectedAuthenticationProfileOwnsItsWireHeaderWithoutAppendingAnotherCredential() {
        HttpRequest request = record(Map.of("authorization", "Bearer CALLER_AUTH_CANARY"));
        assertEquals(List.of("Bearer UNIT_AUTH_CANARY"), request.headers().allValues("Authorization"));
    }

    @Test
    void retainsBothJsonDefaultsWithoutAnOverride() {
        HttpRequest request = record(Map.of());
        assertEquals(List.of("application/json"), request.headers().allValues("Content-Type"));
        assertEquals(List.of("application/json"), request.headers().allValues("Accept"));
    }

    private static HttpRequest record(Map<String, String> headers) {
        var client = new RecordingClient();
        var authenticator = new ConfiguredApiAuthenticator(Map.of("fixture",
                new AuthProfile("fixture", AuthType.BEARER, "UNIT_AUTH_CANARY", null, null, null, Map.of())));
        var transport = new HttpClientTransport(client, authenticator, Duration.ofSeconds(1));
        var request = new ApiRequest(HttpRequest.BodyPublishers.ofString("field=value"), "POST", "/form",
                Map.of(), headers, "fixture", null, "field=value");
        assertThrows(CompletionException.class, () -> transport.send(request, "http://fixture.invalid/form", "fixture").join());
        assertEquals(1, client.calls, "The recorder receives one send; it never opens a connection");
        return client.request;
    }

    private static final class RecordingClient extends HttpClient {
        private HttpRequest request;
        private int calls;

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            throw new UnsupportedOperationException();
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest sent, HttpResponse.BodyHandler<T> handler) {
            request = sent;
            calls++;
            return CompletableFuture.failedFuture(new IOException("Recording boundary; no network"));
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest sent, HttpResponse.BodyHandler<T> handler,
                                                                         HttpResponse.PushPromiseHandler<T> push) {
            return sendAsync(sent, handler);
        }
    }
}
