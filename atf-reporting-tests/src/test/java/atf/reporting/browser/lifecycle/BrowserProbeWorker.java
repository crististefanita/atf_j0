package atf.reporting.browser.lifecycle;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;

/** Child entry point: even SDK construction, newPage and close stay inside the deadline boundary. */
public final class BrowserProbeWorker {
    private BrowserProbeWorker() { }

    public static void main(String[] arguments) throws Throwable {
        Path directory = Path.of(arguments[0]);
        BrowserProbeScope.attach(directory);
        BrowserProbeDiagnostics.startThreadDumpListener(directory);
        BrowserProbe probe = null;
        Throwable failure = null;
        String executionPhase = "SETUP";
        BrowserProbeScope.phase("SETUP");
        try {
            probe = (BrowserProbe) Class.forName(arguments[1]).getConstructor().newInstance();
            probe.open(directory);
            executionPhase = "VERIFY";
            BrowserProbeScope.phase("VERIFY");
            probe.verify(Arrays.copyOfRange(arguments, 2, arguments.length));
        } catch (Throwable error) {
            failure = error;
            Files.writeString(directory.resolve("failure-phase"), executionPhase);
            BrowserProbeScope.recordFailure(error);
        } finally {
            BrowserProbeScope.phase("CLEANUP");
            if (probe != null) {
                try { probe.close(); }
                catch (Throwable error) {
                    if (failure == null) {
                        failure = error;
                        Files.writeString(directory.resolve("failure-phase"), "CLEANUP");
                    }
                    else if (failure != error) failure.addSuppressed(error);
                }
            }
        }
        if (failure != null) BrowserProbeScope.recordFailure(failure);
        BrowserProbeScope.phase(failure == null ? "DONE" : "FAILED");
        if (failure != null) throw failure;
    }
}
