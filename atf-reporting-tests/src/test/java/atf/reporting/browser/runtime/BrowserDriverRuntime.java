package atf.reporting.browser.runtime;

import com.microsoft.playwright.impl.driver.Driver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Prepares one immutable Playwright CLI directory for isolated probe workers. */
public final class BrowserDriverRuntime {
    private static Installation installation;

    private BrowserDriverRuntime() { }

    public static synchronized Installation prepare() throws IOException {
        if (installation != null) return installation;
        String configured = System.getProperty("playwright.cli.dir");
        if (configured != null && !configured.isBlank()) {
            return installation = inspect(Path.of(configured), "system-property", 0);
        }
        long started = System.nanoTime();
        var driver = Driver.ensureDriverInstalled(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"), false);
        Path directory = driver.driverDir().toAbsolutePath().normalize();
        long elapsed = java.time.Duration.ofNanos(System.nanoTime() - started).toMillis();
        installation = inspect(directory, "driver-bundle", elapsed);
        return installation;
    }

    private static Installation inspect(Path directory, String source, long preparationMillis) throws IOException {
        Path cli = directory.resolve("package").resolve("cli.js");
        String node = System.getProperty("os.name", "").toLowerCase().contains("windows")
                ? "node.exe" : "node";
        if (!Files.isRegularFile(cli) || !Files.isRegularFile(directory.resolve(node))) {
            throw new IOException("Playwright CLI directory is incomplete: " + directory);
        }
        return new Installation(directory.toAbsolutePath().normalize(), source, preparationMillis);
    }

    public record Installation(Path directory, String source, long preparationMillis) {
        public Installation {
            if (directory == null || !directory.isAbsolute() || source == null || source.isBlank()
                    || preparationMillis < 0) {
                throw new IllegalArgumentException("Invalid Playwright CLI installation");
            }
        }
    }
}
