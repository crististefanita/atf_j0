package atf.core.http.transport;

import atf.core.http.request.ApiRequest;
import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.testkit.AtfUnitTest;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class HttpClientCancellationTest {
    private static CompletableFuture<atf.core.http.response.ApiResponse> send(ControlledClient client) {
        return new HttpClientTransport(client, new ConfiguredApiAuthenticator(Map.of()), Duration.ofSeconds(1))
                .send(ApiRequest.of("GET", "/read"), "http://fixture.invalid/read", "none");
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void forwardsAcceptedCancellationOnceWithItsInterruptPolicy(boolean interrupt) {
        var client = new ControlledClient();
        var result = send(client);
        assertTrue(result.cancel(interrupt));
        result.cancel(!interrupt);
        assertTrue(result.isCancelled());
        assertTrue(client.pending.isCancelled(), "Mapped cancellation must reach the acquired upstream operation");
        assertEquals(1, client.pending.cancellations.get());
        assertEquals(interrupt, client.pending.interrupt);
        assertFalse(client.pending.complete(response()));
        assertThrows(CancellationException.class, result::join);
        assertEquals(1, client.calls);
    }
    @Test void completedResponseCannotBeReplacedOrCancelled() {
        var client = new ControlledClient(); var result = send(client);
        client.pending.complete(response());
        assertEquals(200, result.join().status());
        assertFalse(result.cancel(true));
        assertEquals(0, client.pending.cancellations.get());
        assertEquals(1, client.calls);
    }
    @Test void originalFailureAndUpstreamCancellationRemainObservable() {
        var client = new ControlledClient(); var result = send(client);
        var failure = new IOException("Controlled transport failure");
        client.pending.completeExceptionally(failure);
        assertSame(failure, assertThrows(CompletionException.class, result::join).getCause());
        assertFalse(result.cancel(true));
        assertEquals(0, client.pending.cancellations.get());
        var cancelled = new ControlledClient(); var downstream = send(cancelled);
        cancelled.pending.cancel(false);
        assertThrows(CancellationException.class, downstream::join);
        assertTrue(downstream.isCancelled());
    }
    @Test void concurrentCompletionAndCancellationHaveOneTerminalOwner() throws Exception {
        try (var workers = Executors.newFixedThreadPool(2)) {
            for (int index = 0; index < 40; index++) {
                var client = new ControlledClient(); var result = send(client);
                var start = new CountDownLatch(1);
                var cancel = workers.submit(() -> { start.await(); return result.cancel(true); });
                var complete = workers.submit(() -> { start.await(); return client.pending.complete(response()); });
                start.countDown();
                boolean accepted = cancel.get(2, TimeUnit.SECONDS);
                complete.get(2, TimeUnit.SECONDS);
                assertTrue(result.isDone());
                assertEquals(accepted ? 1 : 0, client.pending.cancellations.get());
                if (accepted) assertThrows(CancellationException.class, result::join);
                else assertEquals(200, result.join().status());
                assertEquals(1, client.calls);
            }
        }
    }
    @Test void cancellationFailureCannotReplaceTheAcceptedCancellation() {
        var client = new ControlledClient();
        var cleanup = new IllegalStateException("Controlled upstream cleanup failure");
        client.pending.cancelFailure = cleanup;
        var result = send(client);
        assertTrue(result.cancel(true));
        var terminal = assertThrows(CancellationException.class, result::join);
        assertEquals(List.of(cleanup), List.of(terminal.getSuppressed()));
        assertEquals(1, client.pending.cancellations.get());
        assertFalse(client.pending.isDone(), "Best effort cancellation cannot claim upstream cleanup succeeded");
        client.pending.complete(response());
        assertTrue(result.isCancelled());
    }
    private static HttpResponse<String> response() {
        return new HttpResponse<>() {
            public int statusCode() { return 200; }
            public HttpRequest request() { return HttpRequest.newBuilder(URI.create("http://fixture.invalid/read")).build(); }
            public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (name, value) -> true); }
            public String body() { return "{}"; }
            public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
            public URI uri() { return request().uri(); }
            public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
    private static final class Pending extends CompletableFuture<HttpResponse<String>> {
        final AtomicInteger cancellations = new AtomicInteger();
        boolean interrupt;
        RuntimeException cancelFailure;
        @Override public boolean cancel(boolean value) {
            cancellations.incrementAndGet(); interrupt = value;
            if (cancelFailure != null) throw cancelFailure;
            return super.cancel(value);
        }
    }
    private static final class ControlledClient extends HttpClient {
        private final Pending pending = new Pending();
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
            calls++;
            return (CompletableFuture<HttpResponse<T>>) (CompletableFuture<?>) pending;
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest sent, HttpResponse.BodyHandler<T> handler,
                                                                         HttpResponse.PushPromiseHandler<T> push) {
            return sendAsync(sent, handler);
        }
    }
}
