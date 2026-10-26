package atf.reporting.support.request;

import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.request.ApiRequest;
import atf.core.http.transport.UrlConnectionTransport;
import atf.reporting.api.ApiManualReplay;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.api.ReportTransportProvenance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real bounded loopback exchange; expected headers come from the wire, not the renderer. */
public final class LiveClientHeaderProbe {
    private LiveClientHeaderProbe() { }

    public static void verify() throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var executor = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(5000);
            var received = executor.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    assertEquals("GET /wire-defaults HTTP/1.1", input.readLine());
                    var headers = new LinkedHashMap<String, String>();
                    String line;
                    while ((line = input.readLine()) != null && !line.isEmpty()) {
                        int colon = line.indexOf(':');
                        headers.put(line.substring(0, colon).toLowerCase(java.util.Locale.ROOT),
                                line.substring(colon + 1).trim());
                    }
                    socket.getOutputStream().write(
                            "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    return Map.copyOf(headers);
                }
            });
            var authentication = new ApiAuthenticator() {
                @Override public void apply(HttpRequest.Builder request, String profile) { }
                @Override public String describeMode(String profile) { return "none"; }
                @Override public String describeTokenState(String profile) { return "none"; }
            };
            var request = ApiRequest.of("GET", "/wire-defaults");
            String url = "http://127.0.0.1:" + server.getLocalPort() + request.path();
            var response = new UrlConnectionTransport(authentication, Duration.ofSeconds(5))
                    .send(request, url, "none").get(8, TimeUnit.SECONDS);
            assertEquals(204, response.status());
            var wire = received.get(8, TimeUnit.SECONDS);
            var report = ReportModelAdapters.request(request);
            var replay = ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP, report, url, "");
            for (String name : java.util.List.of("Accept", "Content-Type")) {
                assertEquals("application/json", wire.get(name.toLowerCase(java.util.Locale.ROOT)));
                assertEquals(wire.get(name.toLowerCase(java.util.Locale.ROOT)), report.headerEvidence().values().get(name));
                assertTrue(replay.contains(name + ": " + wire.get(name.toLowerCase(java.util.Locale.ROOT))), replay);
            }
        }
    }
}
