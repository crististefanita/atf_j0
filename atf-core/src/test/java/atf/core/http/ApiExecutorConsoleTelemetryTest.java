package atf.core.http;
import atf.testkit.AtfUnitTest;

import atf.core.http.execution.ApiExecutor;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class ApiExecutorConsoleTelemetryTest {
    @Test
    void publishesRequestAndResponseEvidenceAtTheTransportBoundary() {
        var events = new ArrayList<AtfConsoleEvent>();
        ApiTransport transport = (request, url, authProfile) -> CompletableFuture.completedFuture(
                new ApiResponse(201, "{\"id\":1}", Map.of(), Duration.ofMillis(7), url));
        AtfConsoleSink sink = event -> events.add(event);
        var executor = new ApiExecutor(transport, URI.create("https://atf.local"), sink);
        var request = new ApiRequest(
                HttpRequest.BodyPublishers.ofString("{\"name\":\"ATF\"}"),
                "POST", "/users", Map.of(), Map.of(), "none", null,
                "{\"name\":\"ATF\"}");

        var result = executor.execute(request);

        assertAll(
                () -> assertEquals(201, result.response().status()),
                () -> assertEquals(request, result.request(),
                        "the execution result must retain the exact request used by the transport"),
                () -> assertTrue(events.stream().anyMatch(event ->
                        event.type() == AtfConsoleEvent.Type.START
                                && event.scope() == AtfConsoleEvent.Scope.REQUEST
                                && event.name().equals("POST /users"))),
                () -> assertTrue(events.stream().anyMatch(event ->
                        event.type() == AtfConsoleEvent.Type.DETAIL
                                && event.detailName().equals("method")
                                && event.detailValue().equals("POST"))),
                () -> assertTrue(events.stream().anyMatch(event ->
                        event.type() == AtfConsoleEvent.Type.CODE
                                && event.detailName().equals("request.body"))),
                () -> assertTrue(events.stream().anyMatch(event ->
                        event.type() == AtfConsoleEvent.Type.CODE
                                && event.detailName().equals("response.body")
                                && event.detailValue().contains("\"id\"")
                                && event.detailValue().contains("1")), events::toString),
                () -> assertTrue(events.stream().anyMatch(event ->
                        event.type() == AtfConsoleEvent.Type.COMPLETE
                                && event.status() == AtfReportStatus.PASS)));
    }
}
