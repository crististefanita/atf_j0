package atf.core.reporting.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Invocation-owned best-effort diagnostics; nonfatal sink errors never replace execution evidence. */
public final class AtfConsoleDiagnostics {
    private final AtfConsoleSink sink;
    private final List<RuntimeException> failures = new ArrayList<>();

    public AtfConsoleDiagnostics(AtfConsoleSink sink) {
        this.sink = sink == null ? AtfConsoleSink.noOp() : sink;
    }

    public void publish(AtfConsoleEvent event) {
        try { sink.publish(event); }
        catch (RuntimeException failure) { failures.add(failure); }
    }

    /** Success is unchanged; on functional failure the diagnostic causes remain inspectable. */
    public void attachTo(Throwable primary) {
        for (RuntimeException failure : failures) {
            if (failure != primary && Arrays.stream(primary.getSuppressed()).noneMatch(existing -> existing == failure)) {
                primary.addSuppressed(failure);
            }
        }
    }
}
