package atf.debugui;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves that independent ATF Debug UI sessions can coexist without shared state. */
@AtfComponentTest
class DebugUiParallelStartupTest {
    @Test
    void startsAndServesIndependentSessionsInParallel() throws Exception {
        Path root = Files.createTempDirectory("atf-debug-ui-parallel");
        List<Path> roots = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            Path sessionRoot = Files.createDirectory(root.resolve("session-" + index));
            Files.writeString(sessionRoot.resolve("report.html"),
                    "<html><body>session-" + index + "</body></html>");
            roots.add(sessionRoot);
        }

        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = roots.stream().map(sessionRoot -> pool.submit(() -> {
                Path report = sessionRoot.resolve("report.html");
                try (DebugUiServer server = DebugUiServer.start(new DebugUiConfiguration(sessionRoot, 0))) {
                    String response = get(server.uriFor(report));
                    assertTrue(response.startsWith("HTTP/1.1 200 OK"));
                    assertTrue(response.contains(Files.readString(report)));
                }
                return true;
            })).toList();
            for (var task : tasks) assertTrue(task.get());
        } catch (ExecutionException error) {
            throw new AssertionError("Parallel Debug UI session failed", error.getCause());
        }

        try (var files = Files.walk(root)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith("hs_err_pid")));
        }
    }

    private static String get(java.net.URI uri) throws Exception {
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
