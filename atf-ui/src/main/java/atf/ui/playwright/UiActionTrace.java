package atf.ui.playwright;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.Objects;

/**
 * Output port for the human-readable projection of one UI interaction.
 *
 * <p>Playwright action recording and console rendering are deliberately
 * separate concerns: the sequence owns renderer-neutral evidence, while this
 * port lets a composition root add a console sink without giving UI code a
 * {@code PrintStream} or direct stdout ownership.</p>
 */
@FunctionalInterface
public interface UiActionTrace {
    void publish(UiActionEvent event);

    static UiActionTrace noOp() {
        return event -> { };
    }

    static UiActionTrace console(AtfConsoleSink sink) {
        Objects.requireNonNull(sink, "sink");
        return event -> {
            String identity = event.operation() + " -> " + event.target();
            sink.publish(switch (event.kind()) {
                case START -> AtfConsoleEvent.started(AtfConsoleEvent.Scope.UI_ACTION, identity);
                case OBSERVATION -> AtfConsoleEvent.detail(AtfConsoleEvent.Scope.UI_ACTION,
                        "observed", event.observed());
                case COMPLETE -> AtfConsoleEvent.completed(AtfConsoleEvent.Scope.UI_ACTION,
                        identity, event.status(), event.durationMillis());
            });
        };
    }

    record UiActionEvent(Kind kind, String operation, String target, String expected,
                         String observed, AtfReportStatus status, long durationMillis) {
        public UiActionEvent {
            kind = Objects.requireNonNull(kind, "kind");
            operation = required(operation, "operation");
            target = required(target, "target");
            expected = expected == null ? "" : expected.trim();
            observed = observed == null ? "" : observed.trim();
            if (kind == Kind.COMPLETE) status = Objects.requireNonNull(status, "status");
            if (durationMillis < 0) throw new IllegalArgumentException("durationMillis must not be negative");
        }

        private static String required(String value, String field) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
            return normalized;
        }
    }

    enum Kind { START, OBSERVATION, COMPLETE }
}
