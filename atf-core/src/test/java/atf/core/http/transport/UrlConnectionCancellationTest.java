package atf.core.http.transport;

import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.testkit.AtfUnitTest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UrlConnectionCancellationTest {
    private static final Duration DEADLINE = Duration.ofMillis(250);

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void cancellationOwnsTheBodyAndPreventsLateDispatch(boolean interrupt) throws Exception {
        try (var receiver = new Receiver()) {
            var body = new ControlledBody();
            var result = receiver.send(body, Duration.ofSeconds(2));
            try {
                assertTrue(body.subscribed.await(1, TimeUnit.SECONDS));
                assertTrue(result.cancel(interrupt));
                assertTrue(result.isCancelled());
                assertEquals(1, body.cancellations.get(), "Cancellation must reach the acquired body subscription");
                result.cancel(!interrupt);
                assertEquals(1, body.cancellations.get());
                body.release();
                assertFalse(receiver.received.await(250, TimeUnit.MILLISECONDS), "A late body must not dispatch after cancellation");
                assertThrows(CancellationException.class, result::join);
            } finally { result.cancel(true); body.release(); }
        }
    }

    @Test void originalDeadlineIncludesCollectionOfTheBody() throws Exception {
        try (var receiver = new Receiver()) {
            var body = new ControlledBody(); var result = receiver.send(body, DEADLINE);
            try {
                assertTrue(body.subscribed.await(1, TimeUnit.SECONDS));
                var error = assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
                assertInstanceOf(TimeoutException.class, error.getCause());
                assertEquals(1, body.cancellations.get());
                body.release();
                assertFalse(receiver.received.await(250, TimeUnit.MILLISECONDS), "The expired body cannot restart the request");
            } finally { result.cancel(true); body.release(); }
        }
    }

    @Test void cleanupFailureIsSuppressedOnTheOriginalCancellation() throws Exception {
        try (var receiver = new Receiver()) {
            var body = new ControlledBody(); body.cancelFailure = new IllegalStateException("controlled cleanup");
            var result = receiver.send(body, Duration.ofSeconds(2));
            try {
                assertTrue(body.subscribed.await(1, TimeUnit.SECONDS));
                assertTrue(result.cancel(true));
                var error = assertThrows(CancellationException.class, result::join);
                assertEquals(List.of(body.cancelFailure), List.of(error.getSuppressed()));
                assertEquals(1, body.cancellations.get());
            } finally { result.cancel(true); body.release(); }
        }
    }

    @Test void bodyFailureRetainsItsIdentityAndCleanupFailure() throws Exception {
        try (var receiver = new Receiver()) {
            var body = new ControlledBody(); body.cancelFailure = new IllegalStateException("controlled cleanup");
            var original = new IOException("controlled body failure");
            var result = receiver.send(body, Duration.ofSeconds(2));
            try {
                assertTrue(body.subscribed.await(1, TimeUnit.SECONDS));
                body.subscriber.onError(original);
                assertSame(original, assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS)).getCause());
                assertEquals(List.of(body.cancelFailure), List.of(original.getSuppressed()));
                assertEquals(0, receiver.calls.get());
            } finally { result.cancel(true); body.release(); }
        }
    }

    @Test void successfulRequestKeepsBodyHeadersAndOneTerminalResponse() throws Exception {
        try (var receiver = new Receiver()) {
            var result = receiver.send(HttpRequest.BodyPublishers.ofString("request-body"), Duration.ofSeconds(2));
            var response = result.get(2, TimeUnit.SECONDS);
            assertEquals(200, response.status());
            assertEquals("request-body|fixture", response.body());
            assertEquals("present", response.headers().get("X-evidence"));
            assertEquals(1, receiver.calls.get());
            assertFalse(result.cancel(true));
            assertSame(response, result.join());
        }
    }

    @Test void bodyCompletionAndCancellationCompeteForOneTerminalResult() throws Exception {
        try (var receiver = new Receiver(); var workers = Executors.newFixedThreadPool(2)) {
            for (int iteration = 0; iteration < 20; iteration++) {
                var body = new ControlledBody(); var result = receiver.send(body, Duration.ofSeconds(2));
                try {
                    assertTrue(body.subscribed.await(1, TimeUnit.SECONDS));
                    var start = new CountDownLatch(1);
                    var cancel = workers.submit(() -> { start.await(); return result.cancel(true); });
                    var complete = workers.submit(() -> { start.await(); body.release(); return null; });
                    start.countDown();
                    boolean accepted = cancel.get(2, TimeUnit.SECONDS);
                    complete.get(2, TimeUnit.SECONDS);
                    if (accepted) assertThrows(CancellationException.class, result::join);
                    else assertEquals(200, result.get(2, TimeUnit.SECONDS).status());
                    assertEquals(accepted ? 1 : 0, body.cancellations.get());
                } finally { result.cancel(true); body.release(); }
            }
        }
    }

    static final class ControlledBody implements HttpRequest.BodyPublisher {
        final CountDownLatch subscribed = new CountDownLatch(1);
        final AtomicInteger cancellations = new AtomicInteger();
        volatile Flow.Subscriber<? super ByteBuffer> subscriber;
        RuntimeException cancelFailure;
        public long contentLength() { return 1; }
        public void subscribe(Flow.Subscriber<? super ByteBuffer> value) {
            subscriber = value;
            value.onSubscribe(new Flow.Subscription() {
                public void request(long count) { subscribed.countDown(); }
                public void cancel() { cancellations.incrementAndGet(); if (cancelFailure != null) throw cancelFailure; }
            });
        }
        void release() { if (subscriber != null) { subscriber.onNext(ByteBuffer.wrap(new byte[]{'x'})); subscriber.onComplete(); } }
    }

    static final class Receiver implements AutoCloseable {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch received = new CountDownLatch(1);
        final ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();
        final HttpServer server;
        final CountDownLatch responseRelease;
        Receiver() throws IOException { this(new CountDownLatch(0)); }
        Receiver(CountDownLatch responseRelease) throws IOException {
            this.responseRelease = responseRelease;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(handlers);
            server.createContext("/read", exchange -> {
                calls.incrementAndGet(); received.countDown();
                try {
                    String text = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!responseRelease.await(2, TimeUnit.SECONDS)) return;
                    byte[] bytes = (text + "|" + exchange.getRequestHeaders().getFirst("X-fixture")).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("X-evidence", "present");
                    exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
                } catch (IOException cancelledConnection) { /* A claimed dispatch may be cancelled without server rollback. */ }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.start();
        }
        CompletableFuture<ApiResponse> send(HttpRequest.BodyPublisher body, Duration timeout) {
            var request = new ApiRequest(body, "POST", "/read", Map.of(), Map.of("X-fixture", "fixture"), "none", null);
            return new UrlConnectionTransport(new ConfiguredApiAuthenticator(Map.of()), timeout)
                    .send(request, "http://127.0.0.1:" + server.getAddress().getPort() + "/read", "none");
        }
        public void close() throws InterruptedException {
            responseRelease.countDown();
            server.stop(0); handlers.shutdownNow(); assertTrue(handlers.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
