package atf.debugui;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class DebugUiShutdownComponentTest {
    @TempDir Path directory;

    @ParameterizedTest @ValueSource(strings = {"GET /report", "GET /report.html HTTP/1.1\r\nHost: localhost\r\nX-Pending: "})
    void shutdownDoesNotRequireAnIncompleteClientToReleaseItsSocket(String partial) throws Exception {
        Files.writeString(directory.resolve("report.html"), "<p>owned fixture</p>");
        try (var server = DebugUiServer.start(new DebugUiConfiguration(directory, 0));
             var workers = Executors.newVirtualThreadPerTaskExecutor();
             var client = new Socket("127.0.0.1", server.port())) {
            client.getOutputStream().write(partial.getBytes(StandardCharsets.ISO_8859_1)); client.getOutputStream().flush();
            assertReady(server);
            var closing = workers.submit(server::close);
            try { closing.get(2, TimeUnit.SECONDS); }
            finally { client.close(); }
            assertTrue(closing.isDone()); server.close();
        }
    }

    @Test void concurrentCloseReleasesEveryOwnedIdleConnection() throws Exception {
        Files.writeString(directory.resolve("report.html"), "<p>owned fixture</p>");
        var clients = new ArrayList<Socket>();
        try (var server = DebugUiServer.start(new DebugUiConfiguration(directory, 0));
             var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                for (int i = 0; i < 3; i++) clients.add(new Socket("127.0.0.1", server.port()));
                assertReady(server);
                var closes = workers.invokeAll(List.<Callable<Void>>of(() -> { server.close(); return null; },
                        () -> { server.close(); return null; }), 2, TimeUnit.SECONDS);
                assertTrue(closes.stream().noneMatch(Future::isCancelled));
                for (var close : closes) close.get();
            } finally { for (var client : clients) client.close(); }
        }
    }

    @Test void shutdownReleasesAResponseWhoseClientHasStoppedReading() throws Exception {
        var file = directory.resolve("large.bin");
        try (var channel = java.nio.channels.FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            channel.position(32L * 1024 * 1024 - 1); channel.write(java.nio.ByteBuffer.wrap(new byte[]{0}));
        }
        try (var server = DebugUiServer.start(new DebugUiConfiguration(directory, 0));
             var workers = Executors.newVirtualThreadPerTaskExecutor();
             var client = new Socket("127.0.0.1", server.port())) {
            client.setSoTimeout(2000);
            client.getOutputStream().write("GET /large.bin HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            client.getOutputStream().flush(); assertEquals('H', client.getInputStream().read());
            var closing = workers.submit(server::close);
            try { closing.get(2, TimeUnit.SECONDS); }
            finally { client.close(); }
            assertTrue(closing.isDone());
        }
    }

    private static void assertReady(DebugUiServer server) throws Exception {
        try (var request = new Socket("127.0.0.1", server.port())) {
            request.setSoTimeout(2000);
            request.getOutputStream().write("HEAD /report.html HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            request.getOutputStream().flush(); assertEquals('H', request.getInputStream().read());
        }
    }
}
