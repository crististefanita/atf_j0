package atf.reporting.ui.failure;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.util.*;

/** Captured evidence only: no exception-text parsing, browser retry or inferred dispatch/network state. */
public final class UiFailureContext {
    public record Diagnostic(String relation, String text) { }
    private record Pending(Throwable failure, String relation) { }
    private final Throwable failure;
    private final List<AtfUiActionRecord> actions;
    private final AtfUiActionRecord failed;

    public UiFailureContext(Throwable failure, Collection<AtfUiActionRecord> actions) {
        this.failure = failure;
        this.actions = actions == null ? List.of() : actions.stream().filter(Objects::nonNull).toList();
        failed = this.actions.stream().filter(action -> action.status() == AtfReportStatus.FAIL)
                .min(Comparator.comparingInt(AtfUiActionRecord::sequence)).orElse(null);
        if (failure == null && failed == null) {
            throw new IllegalArgumentException("UI failure evidence requires a Throwable or a failed UI action");
        }
    }

    public String summary() {
        if (failed != null) {
            String prefix = failure == null ? "No exception supplied; failed UI "
                    + (failed.source() == AtfUiEvidenceSource.ACTION ? "action: " : "observation: ")
                    : failure.getClass().getSimpleName() + "; first failed evidence: ";
            return prefix + actionSummary(failed);
        }
        return failure.getClass().getSimpleName() + "; failed UI action not recorded; diagnostic: " + message(failure);
    }

    /** Replaces only the selected row's technical preview; original records and the Throwable are untouched. */
    public List<AtfUiActionRecord> projectedActions() {
        return actions.stream().map(action -> action != failed ? action : new AtfUiActionRecord(
                action.sequence(), action.operation(), action.target(), action.expected(), summary(),
                action.durationMillis(), action.status(), action.source())).toList();
    }

    public List<Diagnostic> diagnostics() {
        var result = new ArrayList<Diagnostic>();
        var seen = new IdentityHashMap<Throwable, Integer>();
        var pending = new ArrayDeque<Pending>();
        if (failure != null) pending.add(new Pending(failure, "Primary exception"));
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            Integer previous = seen.get(current.failure());
            if (previous != null) {
                result.add(new Diagnostic(current.relation(), "Reference to diagnostic #" + previous));
                continue;
            }
            int id = result.size() + 1;
            seen.put(current.failure(), id);
            result.add(new Diagnostic(current.relation(), current.failure().getClass().getSimpleName()
                    + ": " + message(current.failure())));
            if (current.failure().getCause() != null) {
                pending.addLast(new Pending(current.failure().getCause(), "Caused by diagnostic #" + id));
            }
            for (Throwable suppressed : current.failure().getSuppressed()) {
                pending.addLast(new Pending(suppressed, "Suppressed by diagnostic #" + id));
            }
        }
        if (failed != null && !failed.observed().isBlank()
                && (failure == null || !failed.observed().equals(failure.getMessage()))) {
            result.add(new Diagnostic("Recorded " + failed.source().name().toLowerCase(Locale.ROOT)
                    + " #" + failed.sequence(), failed.observed()));
        }
        return List.copyOf(result);
    }

    private static String actionSummary(AtfUiActionRecord action) {
        return action.operation() + " #" + action.sequence() + " -> " + action.target()
                + "; elapsed=" + action.durationMillis() + " ms; expected="
                + (action.expected().isBlank() ? "not recorded" : action.expected())
                + (action.source() == AtfUiEvidenceSource.OBSERVE ? "; source=observation" : "");
    }
    private static String message(Throwable failure) {
        return failure.getMessage() == null || failure.getMessage().isBlank() ? "message not recorded" : failure.getMessage();
    }
}
