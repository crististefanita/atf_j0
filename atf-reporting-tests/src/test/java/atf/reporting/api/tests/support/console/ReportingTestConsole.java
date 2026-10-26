package atf.reporting.api.tests.support.console;

import atf.console.AtfConsoleReporter;
import atf.core.reporting.console.AtfConsoleSink;

import java.util.Objects;

/** One configured console shared by browser telemetry and report lifecycle events. */
public final class ReportingTestConsole {
    private final AtfConsoleSink core;
    private final atf.reporting.console.AtfConsoleSink reporting;

    private ReportingTestConsole(AtfConsoleSink core) {
        this.core = Objects.requireNonNull(core, "core");
        reporting = AtfConsoleBridge.toReportingSink(core);
    }

    public static ReportingTestConsole configured() {
        return using(AtfConsoleReporter.configuredStdout());
    }

    public static ReportingTestConsole using(AtfConsoleSink sink) {
        return new ReportingTestConsole(sink);
    }

    public AtfConsoleSink core() { return core; }

    public atf.reporting.console.AtfConsoleSink reporting() { return reporting; }
}
