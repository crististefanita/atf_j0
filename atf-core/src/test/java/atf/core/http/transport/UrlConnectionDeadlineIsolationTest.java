package atf.core.http.transport;

import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.request.ApiRequest;
import atf.testkit.AtfUnitTest;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UrlConnectionDeadlineIsolationTest {
    @Test void pinnedWorkerCannotStarveItsOwnDeadline(@TempDir Path work) throws Exception {
        Path log = work.resolve("deadline.log");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Xmx128m", "-Djava.io.tmpdir=" + work, "-Djdk.virtualThreadScheduler.parallelism=1",
                "-Djdk.virtualThreadScheduler.maxPoolSize=1", "-cp", System.getProperty("java.class.path"),
                PinnedPublisher.class.getName(), work.resolve("deadline.ok").toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(child.waitFor(15, TimeUnit.SECONDS), "Owned child must finish within its diagnostic bound");
            assertEquals(0, child.exitValue(), () -> read(log));
            assertTrue(read(work.resolve("deadline.ok")).contains("ISOLATED_DEADLINE=PASS"));
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); assertTrue(child.waitFor(2, TimeUnit.SECONDS)); }
        }
    }

    private static String read(Path file) {
        try { return Files.readString(file); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }

    public static final class PinnedPublisher {
        public static void main(String[] args) throws Exception {
            // Initialize the fixture before measuring the pinned operation's unchanged 250 ms budget.
            ready();
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var returned = new CountDownLatch(1);
            var cancellations = new AtomicInteger(); var monitor = new Object();
            HttpRequest.BodyPublisher publisher = new HttpRequest.BodyPublisher() {
                public long contentLength() { return 1; }
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        public void request(long count) {
                            synchronized (monitor) {
                                entered.countDown();
                                try { release.await(); }
                                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                                finally { returned.countDown(); }
                            }
                        }
                        public void cancel() { cancellations.incrementAndGet(); }
                    });
                }
            };
            var request = new ApiRequest(publisher, "POST", "/read", Map.of(), Map.of(), "none", null);
            var result = new UrlConnectionTransport(new ConfiguredApiAuthenticator(Map.of()), Duration.ofMillis(250))
                    .send(request, "http://127.0.0.1:1/read", "none");
            try {
                require(entered.await(1, TimeUnit.SECONDS), "Worker must enter the controlled publisher");
                try { result.get(2, TimeUnit.SECONDS); throw new AssertionError("Timeout required"); }
                catch (ExecutionException terminal) { require(terminal.getCause() instanceof TimeoutException, terminal); }
                require(cancellations.get() == 1, "Deadline must cancel the acquired subscription once");
                require(returned.await(1, TimeUnit.SECONDS), "Blocking worker must observe interruption");
                Files.writeString(Path.of(args[0]), "ISOLATED_DEADLINE=PASS; budget=250ms; virtual carriers=1");
            } finally { release.countDown(); result.cancel(true); }
        }
        private static void ready() throws Exception {
            var expected = new java.io.IOException("controlled fixture readiness");
            var body = HttpRequest.BodyPublishers.fromPublisher(subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                public void request(long count) { subscriber.onError(expected); }
                public void cancel() { }
            }));
            var request = new ApiRequest(body, "POST", "/ready", Map.of(), Map.of(), "none", null);
            var result = new UrlConnectionTransport(new ConfiguredApiAuthenticator(Map.of()), Duration.ofSeconds(2))
                    .send(request, "http://127.0.0.1:1/ready", "none");
            try {
                try { result.get(2, TimeUnit.SECONDS); throw new AssertionError("Fixture failure required"); }
                catch (ExecutionException failure) { require(failure.getCause() == expected, failure); }
            } finally { result.cancel(true); }
        }
        private static void require(boolean value, Object message) { if (!value) throw new AssertionError(message); }
    }
}
