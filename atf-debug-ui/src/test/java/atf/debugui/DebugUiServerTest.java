package atf.debugui;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class DebugUiServerTest {
    @Test
    void publishesHtmlAndRejectsTraversal() throws Exception {
        Path root = Files.createTempDirectory("atf-debug-ui");
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

    private static String get(URI uri) throws Exception {
        try (Socket socket = new Socket(uri.getHost(), uri.getPort());
             OutputStream output = socket.getOutputStream()) {
            output.write(("GET " + uri.getRawPath() + " HTTP/1.1\r\nHost: "
                    + uri.getHost() + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            output.flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
