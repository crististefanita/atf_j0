package atf.core.http.transport;

import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.request.ApiRequest;
import atf.testkit.AtfUnitTest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class HttpClientReadCancellationTest {
    @Test void executorCancellationRetainsOneDispatchedLocalReadAndItsTerminalSnapshot() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var cancelled = new AtomicBoolean(); var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try (var handlers = Executors.newVirtualThreadPerTaskExecutor(); var worker = Executors.newSingleThreadExecutor();
             var client = HttpClient.newHttpClient()) {
            server.setExecutor(handlers);
            server.createContext("/read", exchange -> {
                calls.incrementAndGet(); entered.countDown();
                try {
                    if (!release.await(3, TimeUnit.SECONDS)) return;
                    exchange.sendResponseHeaders(200, 2); exchange.getResponseBody().write(new byte[]{'{', '}'});
                } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                catch (java.io.IOException cancelledConnection) { /* Cancellation does not promise a server response. */ }
                finally { exchange.close(); }
            });
            server.start();
            URI origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var transport = new HttpClientTransport(client, new ConfiguredApiAuthenticator(Map.of()), Duration.ofSeconds(2));
            var executor = new ApiExecutor(transport, origin);
            try {
                var invocation = worker.submit(() -> executor.execute(ApiRequest.of("GET", "/read"), Duration.ofSeconds(2), cancelled::get));
                assertTrue(entered.await(2, TimeUnit.SECONDS), "The local handler must observe the dispatched read");
                cancelled.set(true);
                var failure = assertThrows(ExecutionException.class, () -> invocation.get(2, TimeUnit.SECONDS)).getCause();
                assertInstanceOf(CancellationException.class, failure);
                var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
                assertEquals(ApiAttemptOutcome.CANCELLED, snapshot.termination());
                assertEquals(1, snapshot.attempts());
                assertEquals(1, calls.get());
                assertTrue(snapshot.response().isEmpty());
            } finally { release.countDown(); server.stop(0); }
        } finally { release.countDown(); server.stop(0); }
    }
}
