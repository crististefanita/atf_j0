package atf.debugui;
import atf.testkit.AtfComponentTest;


import atf.console.AtfConsoleReporter;
import atf.core.reporting.console.AtfConsoleSink;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the official launcher can serve two independent reports in parallel.
 * The test owns and terminates only the two child launchers it creates; it
 * never discovers or touches any already-running ATF Debug UI process.
 */
@AtfComponentTest
class AtfDebugUiLauncherParallelTest {
    private static final String TARGET_PREFIX = "[ATF] [ATF DEBUG UI] Target for Playwright/TAB MCP: ";
    private static final Pattern ERROR_FILE = Pattern.compile("hs_err_pid\\d+\\.log");

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void officialBoundedLaunchersServeIndependentReportsWithoutNewCrashDumps() throws Exception {
        Path root = Files.createTempDirectory("atf-debug-launcher-parallel-");
        Path first = Files.writeString(root.resolve("first.html"), "<html><body>first</body></html>",
                StandardCharsets.UTF_8);
        Path second = Files.writeString(root.resolve("second.html"), "<html><body>second</body></html>",
                StandardCharsets.UTF_8);
        List<Path> before = crashDumps(root);
        List<Process> children = new ArrayList<>();
        try {
            ReadyLauncher firstLauncher = start(first, root);
            ReadyLauncher secondLauncher = start(second, root);
            children.add(firstLauncher.process());
            children.add(secondLauncher.process());

            URI firstUri = firstLauncher.target().get(10, TimeUnit.SECONDS);
            URI secondUri = secondLauncher.target().get(10, TimeUnit.SECONDS);
            assertNotNull(firstUri);
            assertNotNull(secondUri);
            assertTrue(firstLauncher.process().isAlive(), "first launcher must stay alive");
            assertTrue(secondLauncher.process().isAlive(), "second launcher must stay alive");

            assertBody(firstUri, "first");
            assertBody(secondUri, "second");
            assertTrue(crashDumps(root).equals(before),
                    () -> "launcher produced a crash dump: " + crashDumpSummary(root));
        } finally {
            for (Process child : children) {
                child.destroy();
                if (!child.waitFor(2, TimeUnit.SECONDS)) child.destroyForcibly();
            }
            deleteTree(root);
        }
    }

    private static ReadyLauncher start(Path html, Path root) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java,
                "-Xms16m", "-Xmx64m", "-XX:MaxMetaspaceSize=48m",
                "-Djava.io.tmpdir=" + root,
                "-XX:ErrorFile=" + root.resolve("hs_err_pid%p.log"),
                "-cp", launcherClasspath(),
                AtfDebugUiLauncher.class.getName(), "--html", html.toString(), "--port", "0")
                .redirectErrorStream(true)
                .start();
        CompletableFuture<URI> target = CompletableFuture.supplyAsync(() -> readTarget(process));
        return new ReadyLauncher(process, target);
    }

    private static String launcherClasspath() {
        try {
            List<String> locations = List.of(
                    codeSource(AtfDebugUiLauncher.class),
                    codeSource(AtfConsoleReporter.class),
                    codeSource(AtfConsoleSink.class),
                    codeSource(JsonView.class),
                    codeSource(JsonFactory.class),
                    codeSource(ObjectMapper.class),
                    codeSource(YAMLFactory.class),
                    codeSource(Yaml.class));
            String current = System.getProperty("java.class.path", "");
            return String.join(java.io.File.pathSeparator, locations)
                    + java.io.File.pathSeparator + current;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot resolve ATF Debug UI launcher classes", error);
        }
    }

    private static String codeSource(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    }

    private static URI readTarget(Process process) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = process.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                if (line.startsWith(TARGET_PREFIX)) return URI.create(line.substring(TARGET_PREFIX.length()));
            }
            throw new IllegalStateException("ATF Debug launcher exited before publishing its target; output="
                    + output + "; exit=" + process.exitValue());
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read ATF Debug launcher output", error);
        }
    }

    private static void assertBody(URI uri, String expected) throws Exception {
        var connection = (java.net.HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        connection.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            assertEquals(200, connection.getResponseCode());
            assertTrue(reader.lines().reduce("", (left, right) -> left + right)
                    .contains(expected));
        } finally {
            connection.disconnect();
        }
    }

    private static List<Path> crashDumps(Path root) throws IOException {
        try (var paths = Files.list(root)) {
            return paths.filter(path -> ERROR_FILE.matcher(path.getFileName().toString()).matches())
                    .sorted().toList();
        }
    }

    private static String crashDumpSummary(Path root) {
        try {
            return crashDumps(root).toString();
        } catch (IOException error) {
            return "<cannot inspect crash-dump directory: " + error.getMessage() + ">";
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    throw new IllegalStateException(error);
                }
            });
        }
    }

    private record ReadyLauncher(Process process, CompletableFuture<URI> target) { }
}
