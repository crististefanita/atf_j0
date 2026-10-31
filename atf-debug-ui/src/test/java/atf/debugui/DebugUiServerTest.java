package atf.debugui;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class DebugUiServerTest {
    @Test
    void publishesHtmlAndRejectsTraversal(@TempDir Path directory) throws Exception {
        Path root = Files.createDirectory(directory.resolve("reports"));
        Path html = Files.writeString(root.resolve("report.html"), "<html><body>ATF debug</body></html>");
        Path outside = Files.writeString(root.getParent().resolve("outside.txt"), "private");

        try (DebugUiServer server = DebugUiServer.start(new DebugUiConfiguration(root, 0))) {
            String page = get(server.uriFor(html));
            assertTrue(page.startsWith("HTTP/1.1 200 OK"));
            assertTrue(page.contains("ATF debug"));

            URI traversal = URI.create(server.baseUri() + "%2E%2E/" + outside.getFileName());
            assertTrue(get(traversal).startsWith("HTTP/1.1 403 Forbidden"));
        }
    }

    @Test
    void literalPlusEncodedPlusSpaceAndEncodedPercentKeepDistinctFileIdentities(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("proof+1.html"), "PLUS_ONLY");
        Files.writeString(directory.resolve("proof 1.html"), "SPACE_ONLY_LONGER");
        Files.writeString(directory.resolve("proof%2B1.html"), "PERCENT_ONLY");
        try (var server = DebugUiServer.start(new DebugUiConfiguration(directory, 0))) {
            for (var entry : Map.of("/proof+1.html", "PLUS_ONLY", "/proof%2B1.html", "PLUS_ONLY",
                    "/proof%201.html", "SPACE_ONLY_LONGER", "/proof%252B1.html", "PERCENT_ONLY").entrySet()) {
                String response = request(server.baseUri(), "GET", entry.getKey());
                assertTrue(response.startsWith("HTTP/1.1 200 OK"), entry.getKey());
                assertEquals(entry.getValue(), body(response), entry.getKey());
                String head = request(server.baseUri(), "HEAD", entry.getKey());
                assertTrue(head.startsWith("HTTP/1.1 200 OK"), entry.getKey());
                assertTrue(head.contains("Content-Length: " + entry.getValue().length() + "\r\n"), entry.getKey());
                assertEquals("", body(head), "HEAD has metadata without a response body");
            }
        }
    }

    @Test
    void malformedPathsAndEncodedTraversalRemainForbiddenWithoutDoubleDecoding(@TempDir Path directory) throws Exception {
        Path root = Files.createDirectory(directory.resolve("reports"));
        Files.writeString(directory.resolve("outside.html"), "outside");
        try (var server = DebugUiServer.start(new DebugUiConfiguration(root, 0))) {
            for (String path : new String[]{"/%2E%2E/outside.html", "/%ZZ.html", "//outside.html", "/report.html#fragment"}) {
                assertTrue(request(server.baseUri(), "GET", path).startsWith("HTTP/1.1 403 Forbidden"), path);
                String head = request(server.baseUri(), "HEAD", path);
                assertTrue(head.startsWith("HTTP/1.1 403 Forbidden"), path);
                assertEquals("", body(head), "A refused HEAD preserves error metadata without a body");
            }
        }
    }

    private static String body(String response) {
        return response.substring(response.indexOf("\r\n\r\n") + 4);
    }

    private static String get(URI uri) throws Exception {
        return request(uri, "GET", uri.getRawPath());
    }

    private static String request(URI uri, String method, String path) throws Exception {
        try (Socket socket = new Socket(uri.getHost(), uri.getPort());
             OutputStream output = socket.getOutputStream()) {
            socket.setSoTimeout(3000);
            output.write((method + " " + path + " HTTP/1.1\r\nHost: "
                    + uri.getHost() + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            output.flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
