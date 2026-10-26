package atf.debugui;

import atf.console.AtfConsoleReporter;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/** IntelliJ/Maven entry point that publishes one HTML file and prints the TAB MCP target. */
public final class AtfDebugUiLauncher {
    private AtfDebugUiLauncher() { }

    public static void main(String[] args) throws InterruptedException {
        DebugUiMemoryPolicy.enforce();
        Arguments arguments = Arguments.parse(args);
        try (DebugUiServer server = DebugUiServer.start(
                DebugUiConfiguration.forHtml(arguments.html(), arguments.port()))) {
            AtfConsoleReporter console = AtfConsoleReporter.configuredStdout();
            console.info("[ATF DEBUG UI] Read-only loopback server started");
            console.info("[ATF DEBUG UI] Target for Playwright/TAB MCP: "
                    + server.uriFor(arguments.html()));
            console.info("[ATF DEBUG UI] Root: " + arguments.html().toAbsolutePath().normalize().getParent());
            console.info("[ATF DEBUG UI] Stop with Ctrl+C");
            new CountDownLatch(1).await();
        }
    }

    private record Arguments(Path html, int port) {
        static Arguments parse(String[] args) {
            Path html = null;
            int port = 0;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--html" -> html = Path.of(requiredValue(args, ++i, "--html"));
                    case "--port" -> port = Integer.parseInt(requiredValue(args, ++i, "--port"));
                    case "--help" -> throw usage(null);
                    default -> throw usage("Unknown option: " + args[i]);
                }
            }
            if (html == null) throw usage("--html is required");
            return new Arguments(html, port);
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) throw usage(option + " requires a value");
            return args[index];
        }

        private static IllegalArgumentException usage(String error) {
            String prefix = error == null ? "" : error + System.lineSeparator();
            return new IllegalArgumentException(prefix + "Usage: AtfDebugUiLauncher --html <path> [--port <0-65535>]");
        }
    }
}
