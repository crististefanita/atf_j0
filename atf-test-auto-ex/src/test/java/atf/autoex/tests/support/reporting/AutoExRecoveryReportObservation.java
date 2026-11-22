package atf.autoex.tests.support.reporting;

import atf.core.ui.AtfUiActionRecord;
import atf.testkit.assertions.ComponentAuditObservation;
import atf.ui.recovery.UiRecoveryTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Projects the consumer-owned typed recovery trace into the component report fields. */
public final class AutoExRecoveryReportObservation {
    private AutoExRecoveryReportObservation() { }

    public static ComponentAuditObservation create(List<UiRecoveryTrace> traces,
                                                   List<AtfUiActionRecord> actions) {
        Objects.requireNonNull(traces, "traces");
        Objects.requireNonNull(actions, "actions");
        if (traces.isEmpty() || traces.size() != actions.size()) {
            throw new IllegalArgumentException("Each recovery trace must have one matching INFO action row");
        }

        var fields = new ArrayList<ComponentAuditObservation.Field>(traces.size() * 6);
        for (int index = 0; index < traces.size(); index++) {
            var trace = traces.get(index);
            var action = actions.get(index);
            int attempt = index + 1;
            if (trace.attempt() != attempt || action.sequence() != attempt
                    || !trace.operation().equals(action.target()) || !"INFO".equals(action.status().name())) {
                throw new IllegalArgumentException("Recovery trace and INFO action order/operation must match at attempt " + attempt);
            }

            String prefix = "Attempt " + attempt + " · ";
            fields.add(new ComponentAuditObservation.Field(prefix + "Operation", trace.operation()));
            fields.add(new ComponentAuditObservation.Field(prefix + "Event", readableEvent(action.operation(), attempt)));
            fields.add(new ComponentAuditObservation.Field(prefix + "Policy", action.expected()));
            fields.add(new ComponentAuditObservation.Field(prefix + "Observation", observation(trace)));
            fields.add(new ComponentAuditObservation.Field(prefix + "Timing", timing(trace)));
            fields.add(new ComponentAuditObservation.Field(prefix + "Result", action.status().name()));
        }

        UiRecoveryTrace last = traces.getLast();
        if (last.decision() != UiRecoveryTrace.Decision.RECOVERED
                && last.decision() != UiRecoveryTrace.Decision.EXHAUSTED) {
            throw new IllegalArgumentException("The report requires a terminal recovery decision");
        }
        return new ComponentAuditObservation("Backend recovery trace · " + last.decision(), fields);
    }

    private static String readableEvent(String operation, int attempt) {
        String expected = "BE observation #" + attempt;
        if (!expected.equals(operation)) {
            throw new IllegalArgumentException("Unexpected recovery evidence event for attempt " + attempt);
        }
        return "Backend observation #" + attempt;
    }

    private static String observation(UiRecoveryTrace trace) {
        String signal = trace.signal().map(value -> value.description())
                .orElse("backend signal not captured");
        return trace.decision() + "; " + trace.reason() + "; " + signal
                + "; observation only; no business action replay";
    }

    private static String timing(UiRecoveryTrace trace) {
        return "probe=" + trace.probeTime() + "; backoff=" + trace.backoffTime()
                + "; total=" + trace.totalTime();
    }
}
