package atf.core.http;

import atf.core.http.execution.ApiExecutor;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.testkit.AtfUnitTest;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ApiExecutorConsoleRedactionTest {
    @Test void everyConsoleChannelIsSafeWhileExecutionRetainsOriginalValues() {
        String canary = UUID.randomUUID().toString();
        String path = "/token/" + canary;
        String body = "{\"message\":\"Authorization: Bearer " + canary + "\",\"value\":9007199254740993.01}";
        var request = new ApiRequest(HttpRequest.BodyPublishers.ofString(body), "POST", path,
                Map.of("api_key", canary), Map.of(), "none", null, body);
        var events = new ArrayList<AtfConsoleEvent>();
        var calls = new AtomicInteger();
        ApiTransport transport = (actual, url, auth) -> {
            calls.incrementAndGet();
            assertSame(request, actual);
            assertTrue(actual.path().endsWith(canary), "Execution path must stay complete");
            assertTrue(actual.bodyText().contains(canary), "Execution body must stay complete");
            return CompletableFuture.completedFuture(new ApiResponse(200, body, Map.of(), Duration.ZERO, url));
        };
        var result = new ApiExecutor(transport, URI.create("https://fixture.invalid"), (AtfConsoleSink) events::add).execute(request);
        assertEquals(1, calls.get());
        assertSame(request, result.request());
        assertTrue(result.response().body().contains(canary), "Response facts must stay complete");
        assertEquals(2, events.stream().filter(event -> event.type() == AtfConsoleEvent.Type.CODE).count());
        assertEquals(2, events.stream().filter(event -> event.type() == AtfConsoleEvent.Type.START
                || event.type() == AtfConsoleEvent.Type.COMPLETE).count());
        assertTrue(events.stream().noneMatch(event -> (event.name() + event.detailValue()).contains(canary)),
                "Synthetic credential must be absent from every emitted channel");
        assertTrue(events.stream().filter(event -> event.type() == AtfConsoleEvent.Type.CODE)
                .allMatch(event -> event.detailValue().contains("9007199254740993.01")),
                "Non-sensitive numeric neighbors must keep their exact token");
    }
}
