package atf.console;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Stream adapter for the ATF console projection.
 *
 * <p>Configuration, stream capability detection, lifecycle indentation, and
 * event formatting are deliberately delegated to independent collaborators.
 * Console diagnostics remain best-effort and can never change a test verdict.</p>
 */
public final class AtfConsoleReporter implements AtfConsoleSink {
    private static final String PREFIX = "[ATF] ";

    private final PrintStream output;
    private final ConsoleSettings settings;
    private final boolean reportPathEnabled;
    private final AtfConsoleFormatter formatter;
    private final ConsoleLifecycleState lifecycle = new ConsoleLifecycleState();

    public AtfConsoleReporter(PrintStream output, ConsoleSettings settings) {
        this(output, settings, false);
    }

    /** Creates a reporter that may print the final report path when explicitly opted in. */
    public AtfConsoleReporter(PrintStream output, ConsoleSettings settings, boolean reportPathEnabled) {
        this.output = Objects.requireNonNull(output, "output");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.reportPathEnabled = reportPathEnabled;
        this.formatter = new AtfConsoleFormatter(ConsoleOutputStyle.forStream(output));
    }

    /** Creates the configured process-console adapter; stdout ownership stays here. */
    public static AtfConsoleReporter configuredStdout() {
        return new AtfConsoleReporter(System.out, AtfConsoleConfigurationLoader.load().settings());
    }

    /** Creates the configured process-console adapter with a per-test projection policy. */
    public static AtfConsoleReporter configuredStdout(ConsoleSettings settings) {
        return new AtfConsoleReporter(System.out, settings);
    }

    /** Creates the configured adapter with an explicit report-path opt-in. */
    public static AtfConsoleReporter configuredStdout(ConsoleSettings settings,
                                                       boolean reportPathEnabled) {
        return new AtfConsoleReporter(System.out, settings, reportPathEnabled);
    }

    /** Publishes one launcher/diagnostic line without leaking a PrintStream to callers. */
    public synchronized void info(String message) {
        if (!settings.enabled()) return;
        try {
            output.println(PREFIX + Objects.requireNonNullElse(message, ""));
        } catch (RuntimeException ignored) {
            // Console output is diagnostic; it must never change a test verdict.
        }
    }

    @Override
    public synchronized void publish(AtfConsoleEvent event) {
        Objects.requireNonNull(event, "event");
        if (!settings.enabled()) return;
        if (event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED && !reportPathEnabled) return;
        try {
            int eventDepth = lifecycle.depthFor(event.type());
            for (String line : formatter.format(event, settings.detailsEnabled())) {
                output.println(PREFIX + "  ".repeat(Math.max(0, eventDepth)) + line);
            }
        } catch (RuntimeException ignored) {
            // Console output is diagnostic; it must never change a test verdict.
        }
    }
}
