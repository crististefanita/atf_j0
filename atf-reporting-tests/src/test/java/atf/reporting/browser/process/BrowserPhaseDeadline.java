package atf.reporting.browser.process;

import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import java.util.function.LongSupplier;

/** Event identity preserves separate phase scopes even when the watcher misses an intermediate marker. */
public final class BrowserPhaseDeadline {
    private final LongSupplier clock;
    private String event = "";
    private String phase = "SETUP";
    private long started;
    private Long workerStarted;
    private boolean overrun;

    public BrowserPhaseDeadline() { this(System::nanoTime); }
    public BrowserPhaseDeadline(LongSupplier clock) { this.clock = clock; started = clock.getAsLong(); }

    public void observe(BrowserPhaseChannel.Frame frame, BrowserProbeBudget budget) {
        if (overrun || frame.event().equals(event)) return;
        if (workerStarted != null && frame.workerNanos() - workerStarted >= budget.forPhase(phase).toNanos()) {
            overrun = true;
            return;
        }
        if (workerStarted == null || renews(frame.event())) workerStarted = frame.workerNanos();
        observe(frame.event());
    }

    public void observe(String nextEvent) {
        if (nextEvent.equals(event)) return;
        String raw = nextEvent.substring(15);
        boolean terminal = raw.equals("DONE") || raw.equals("FAILED");
        String nextPhase = terminal ? "CLEANUP" : raw;
        if (renews(nextEvent)) started = clock.getAsLong();
        phase = nextPhase; event = nextEvent;
    }

    private boolean renews(String nextEvent) {
        String next = nextEvent.substring(15);
        if (event.isEmpty() && next.equals("SETUP")) return false;
        if (next.equals("DONE") || next.equals("FAILED")) return !phase.equals("CLEANUP");
        return !((phase.equals("DISCOVERY") && next.equals("VERIFY"))
                || (phase.equals("VERIFY") && next.equals("DISCOVERY")));
    }

    public String phase() { return phase; }
    public long startedAt() { return started; }
    public long elapsedNanos() { return clock.getAsLong() - started; }
    public boolean expired(BrowserProbeBudget budget) { return overrun || elapsedNanos() >= budget.forPhase(phase).toNanos(); }
}
