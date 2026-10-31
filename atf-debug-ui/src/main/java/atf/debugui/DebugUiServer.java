package atf.debugui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publishes one local artifact tree on loopback for manual Playwright/TAB MCP inspection.
 * It is deliberately read-only: no report generation, HTML mutation, or external binding.
 */
public final class DebugUiServer implements AutoCloseable {
    private static final String LOOPBACK_HOST = "127.0.0.1";
    private final Path rootDirectory;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final java.net.URI baseUri;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();

    private DebugUiServer(Path rootDirectory, ServerSocket serverSocket, ExecutorService executor) {
        this.rootDirectory = rootDirectory;
        this.serverSocket = serverSocket;
        this.executor = executor;
        this.baseUri = java.net.URI.create("http://" + LOOPBACK_HOST + ":" + serverSocket.getLocalPort() + "/");
    }

    public static DebugUiServer start(DebugUiConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        try {
            Path root = configuration.rootDirectory().toRealPath();
            ServerSocket socket = new ServerSocket();
            socket.bind(new InetSocketAddress(LOOPBACK_HOST, configuration.port()));
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            DebugUiServer server = new DebugUiServer(root, socket, executor);
            executor.submit(server::acceptLoop);
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot start ATF Debug UI loopback server", error);
        }
    }

    public java.net.URI baseUri() { return baseUri; }

    public int port() { return serverSocket.getLocalPort(); }

    public java.net.URI uriFor(Path artifact) {
        Path file = Objects.requireNonNull(artifact, "artifact").toAbsolutePath().normalize();
        try {
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(rootDirectory) || !Files.isRegularFile(realFile)) {
                throw new IllegalArgumentException("Artifact is outside the debug root: " + file);
            }
            String relative = rootDirectory.relativize(realFile).toString().replace('\\', '/');
            return java.net.URI.create(baseUri + encodePath(relative));
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot resolve debug artifact: " + file, error);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        closeSocket(serverSocket);
        synchronized (clients) { clients.forEach(DebugUiServer::closeSocket); }
        executor.close();
    }

    private void acceptLoop() {
        while (!closed.get()) {
            try {
                Socket socket = serverSocket.accept();
                synchronized (clients) {
                    if (closed.get()) { closeSocket(socket); continue; }
                    clients.add(socket);
                }
                try { executor.submit(() -> handle(socket)); }
                catch (RejectedExecutionException stopping) {
                    clients.remove(socket); closeSocket(socket);
                    if (!closed.get()) throw stopping;
                }
            } catch (IOException error) {
                if (!closed.get()) throw new IllegalStateException("ATF Debug UI accept loop failed", error);
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
             OutputStream output = socket.getOutputStream()) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) return;
            String[] parts = requestLine.split(" ", 3);
            String method = parts[0];
            if (parts.length < 2) {
                respond(output, method, 400, "Bad Request", "text/plain; charset=utf-8", "Bad Request");
                return;
            }
            String rawTarget = parts[1];
            String header;
            while ((header = reader.readLine()) != null && !header.isEmpty()) { }
            if (!method.equals("GET") && !method.equals("HEAD")) {
                respond(output, method, 405, "Method Not Allowed", "text/plain; charset=utf-8", "Method Not Allowed");
                return;
            }
            Path file = resolve(rootDirectory, rawTarget);
            if (file == null) {
                respond(output, method, 403, "Forbidden", "text/plain; charset=utf-8", "Forbidden");
                return;
            }
            if (!Files.isRegularFile(file)) {
                respond(output, method, 404, "Not Found", "text/plain; charset=utf-8", "Not Found");
                return;
            }
            long length = Files.size(file);
            output.write(("HTTP/1.1 200 OK\r\nContent-Type: " + contentType(file)
                    + "\r\nCache-Control: no-store\r\nContent-Length: " + length
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            if (method.equals("GET")) Files.copy(file, output);
            output.flush();
        } catch (Exception ignored) {
            // A debug tab may be closed while a response is being streamed.
        } finally {
            clients.remove(socket);
        }
    }

    private static void closeSocket(java.io.Closeable socket) {
        try { socket.close(); } catch (IOException ignored) { }
    }

    private static void respond(OutputStream output, String method, int status, String reason,
                                String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        output.write(("HTTP/1.1 " + status + " " + reason + "\r\nContent-Type: " + contentType
                + "\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        if (!method.equals("HEAD")) output.write(bytes);
        output.flush();
    }

    private static Path resolve(Path root, String rawTarget) {
        try {
            var target = java.net.URI.create(rawTarget == null ? "/" : rawTarget);
            if (target.isAbsolute() || target.getRawAuthority() != null || target.getRawFragment() != null) return null;
            String decoded = target.getPath();
            if (decoded == null || !decoded.startsWith("/") || decoded.equals("/")) return null;
            Path candidate = root.resolve(decoded.substring(1)).normalize();
            if (!candidate.startsWith(root)) return null;
            Path real = candidate.toRealPath();
            return real.startsWith(root) ? real : null;
        } catch (RuntimeException | IOException error) {
            return null;
        }
    }

    private static String encodePath(String relative) {
        return java.util.Arrays.stream(relative.split("/", -1))
                .map(segment -> java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8)
                        .replace("+", "%20").replace("%2F", "/"))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
