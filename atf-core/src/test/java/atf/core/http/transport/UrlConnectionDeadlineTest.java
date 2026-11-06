package atf.core.http.transport;

import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiAttemptOutcome;
import atf.testkit.AtfUnitTest;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UrlConnectionDeadlineTest {
    @Test void callerDeadlineCancelsBodyEvenWhenTheTransportBudgetIsLonger() throws Exception {
        try (var receiver = new UrlConnectionCancellationTest.Receiver()) {
            var body = new UrlConnectionCancellationTest.ControlledBody();
            var transport = new UrlConnectionTransport(new ConfiguredApiAuthenticator(Map.of()), Duration.ofSeconds(2));
            var executor = new ApiExecutor(transport, URI.create("http://127.0.0.1:" + receiver.server.getAddress().getPort()));
            var request = new ApiRequest(body, "POST", "/read", Map.of(), Map.of(), "none", null);
            try {
                var failure = assertThrows(RuntimeException.class,
                        () -> executor.execute(request, Duration.ofMillis(250), () -> false));
                var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
                assertEquals(ApiAttemptOutcome.TIMED_OUT, snapshot.termination());
                assertEquals(1, snapshot.attempts(), "Executor invocation count is distinct from a network dispatch");
                assertEquals(1, body.cancellations.get());
                assertTrue(snapshot.response().isEmpty());
                body.release();
                assertFalse(receiver.received.await(250, TimeUnit.MILLISECONDS));
            } finally { body.release(); }
        }
    }

    @Test void subscriptionArrivingAfterCancellationIsCancelledWithoutDemand() throws Exception {
        try (var receiver = new UrlConnectionCancellationTest.Receiver()) {
            var attached = new CompletableFuture<Flow.Subscriber<? super ByteBuffer>>();
            HttpRequest.BodyPublisher publisher = new HttpRequest.BodyPublisher() {
                public long contentLength() { return 1; }
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) { attached.complete(subscriber); }
            };
            var result = receiver.send(publisher, Duration.ofSeconds(2));
            try {
                var subscriber = attached.get(1, TimeUnit.SECONDS);
                assertTrue(result.cancel(false));
                var cancelled = new AtomicInteger(); var demand = new AtomicInteger();
                subscriber.onSubscribe(new Flow.Subscription() {
                    public void request(long count) { demand.incrementAndGet(); }
                    public void cancel() { cancelled.incrementAndGet(); }
                });
                subscriber.onNext(ByteBuffer.wrap(new byte[]{'x'})); subscriber.onComplete();
                assertEquals(1, cancelled.get()); assertEquals(0, demand.get());
                assertFalse(receiver.received.await(250, TimeUnit.MILLISECONDS));
            } finally { result.cancel(true); }
        }
    }

    @Test void deadlineInterruptsABlockingPublisherAndRetainsCleanupFailure() throws Exception {
        try (var receiver = new UrlConnectionCancellationTest.Receiver()) {
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var returned = new CountDownLatch(1);
            var cleanup = new IllegalStateException("controlled subscription cleanup");
            var cancelled = new AtomicInteger();
            HttpRequest.BodyPublisher publisher = new HttpRequest.BodyPublisher() {
                public long contentLength() { return 1; }
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        public void request(long count) {
                            entered.countDown();
                            try { release.await(); }
                            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                            finally { returned.countDown(); }
                        }
                        public void cancel() { cancelled.incrementAndGet(); throw cleanup; }
                    });
                }
            };
            var result = receiver.send(publisher, Duration.ofMillis(250));
            try {
                assertTrue(entered.await(1, TimeUnit.SECONDS));
                var failure = assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS)).getCause();
                assertInstanceOf(TimeoutException.class, failure);
                assertArrayEquals(new Throwable[]{cleanup}, failure.getSuppressed());
                assertEquals(1, cancelled.get());
                assertTrue(returned.await(1, TimeUnit.SECONDS), "The owned blocking worker must observe interruption");
                assertEquals(0, receiver.calls.get());
            } finally { release.countDown(); result.cancel(true); }
        }
    }

    @Test void cancellationAfterDispatchDoesNotClaimServerRollback() throws Exception {
        try (var receiver = new UrlConnectionCancellationTest.Receiver(new CountDownLatch(1))) {
            var result = receiver.send(HttpRequest.BodyPublishers.noBody(), Duration.ofSeconds(2));
            try {
                assertTrue(receiver.received.await(1, TimeUnit.SECONDS));
                assertTrue(result.cancel(true));
                assertThrows(CancellationException.class, result::join);
                assertEquals(1, receiver.calls.get());
            } finally { result.cancel(true); }
        }
    }

    @Test void transportRejectsAnUnboundedOrNegativeTimeout() {
        var auth = new ConfiguredApiAuthenticator(Map.of());
        assertThrows(IllegalArgumentException.class, () -> new UrlConnectionTransport(auth, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new UrlConnectionTransport(auth, Duration.ofMillis(-1)));
    }
}
