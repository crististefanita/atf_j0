package atf.reporting.browser.process;

import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import java.util.function.LongSupplier;

/** Event identity preserves separate phase scopes even when the watcher misses an intermediate marker. */
public final class BrowserPhaseDeadline {
    private final LongSupplier clock;
    private String event = "";
    private String phase = "SETUP";
    private long started;

    public BrowserPhaseDeadline() { this(System::nanoTime); }
    public BrowserPhaseDeadline(LongSupplier clock) { this.clock = clock; started = clock.getAsLong(); }

    public void observe(String nextEvent) {
        if (nextEvent.equals(event)) return;
        String raw = nextEvent.substring(15);
        boolean terminal = raw.equals("DONE") || raw.equals("FAILED");
        String nextPhase = terminal ? "CLEANUP" : raw;
        if (!terminal || !phase.equals("CLEANUP")) started = clock.getAsLong();
        phase = nextPhase; event = nextEvent;
    }

    public String phase() { return phase; }
    public long startedAt() { return started; }
    public long elapsedNanos() { return clock.getAsLong() - started; }
    public boolean expired(BrowserProbeBudget budget) { return elapsedNanos() >= budget.forPhase(phase).toNanos(); }
}
