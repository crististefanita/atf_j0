package atf.reporting.browser.lifecycle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import atf.reporting.browser.process.BrowserPhaseChannel;

/** Files are confined to the unique probe directory; stdout is not the protocol. */
final class BrowserProbeDiagnostics {
    private static final AtomicLong EVENTS = new AtomicLong();
    private BrowserProbeDiagnostics() { }

    static void phase(Path directory, String phase) throws Exception {
        String name = "phase-" + String.format(java.util.Locale.ROOT, "%08d", EVENTS.incrementAndGet()) + "-" + phase;
        BrowserPhaseChannel.publish(directory, name, System.nanoTime());
        if (phase.equals("DONE") || phase.equals("FAILED")) Files.writeString(directory.resolve("phase-" + phase), name);
    }

    static void failure(Path directory, Throwable error) throws Exception {
        Files.writeString(directory.resolve("failure.log"), stack(error));
    }

    static void cleanupFailure(Path directory, Throwable error) throws Exception {
        Files.writeString(directory.resolve("failure.log"), "\nCleanup failure:\n" + stack(error), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String stack(Throwable error) {
        var text = new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        return text.toString();
    }

    static void startThreadDumpListener(Path directory) {
        var listener = new Thread(() -> {
            try {
                while (!Files.exists(directory.resolve("diagnose"))) Thread.sleep(10);
                var text = new StringBuilder();
                Thread.getAllStackTraces().forEach((thread, stack) -> {
                    text.append(thread.getName()).append(" ").append(thread.getState()).append('\n');
                    for (var frame : stack) text.append("  at ").append(frame).append('\n');
                });
                Files.writeString(directory.resolve("threads.log"), text);
            } catch (Exception error) {
                try { Files.writeString(directory.resolve("diagnostic-error.log"), error.toString()); }
                catch (Exception ignored) { /* Process log remains available. */ }
            }
        }, "report-probe-diagnostics");
        listener.setDaemon(true);
        listener.start();
    }
}
