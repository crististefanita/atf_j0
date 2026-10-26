package atf.reporting.api.tests.support.console;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.Objects;
import java.nio.file.Path;

import static atf.reporting.console.AtfConsoleEvent.code;
import static atf.reporting.console.AtfConsoleEvent.completed;
import static atf.reporting.console.AtfConsoleEvent.detail;
import static atf.reporting.console.AtfConsoleEvent.started;
import static atf.reporting.console.AtfConsoleEvent.reportPublished;

/**
 * Test-composition adapter for the two renderer-neutral console ports.
 *
 * <p>The adapter belongs to {@code atf-reporting-tests}, which is the
 * composition root that is allowed to consume both ports. Neither standalone
 * production module depends on the other, and Practice Shop does not own a
 * cross-module conversion.</p>
 */
public final class AtfConsoleBridge {
    private AtfConsoleBridge() {
    }

    public static atf.reporting.console.AtfConsoleSink toReportingSink(AtfConsoleSink coreSink) {
        Objects.requireNonNull(coreSink, "coreSink");
        return event -> coreSink.publish(toCoreEvent(event));
    }

    static AtfConsoleSink toCoreSink(atf.reporting.console.AtfConsoleSink reportingSink) {
        Objects.requireNonNull(reportingSink, "reportingSink");
        return event -> reportingSink.publish(toReportingEvent(event));
    }

    private static AtfConsoleEvent toCoreEvent(atf.reporting.console.AtfConsoleEvent event) {
        Objects.requireNonNull(event, "event");
        return AtfConsoleEvent.fromExternalEnums(event.type(), event.scope(), event.name(),
                event.status(), event.durationMillis(), event.detailName(), event.detailValue());
    }

    private static atf.reporting.console.AtfConsoleEvent toReportingEvent(AtfConsoleEvent event) {
        Objects.requireNonNull(event, "event");
        return switch (event.type()) {
            case START -> started(reportingScope(event.scope()), event.name());
            case DETAIL -> detail(reportingScope(event.scope()), event.detailName(), event.detailValue());
            case CODE -> code(reportingScope(event.scope()), event.detailName(), event.detailValue());
            case COMPLETE -> completed(reportingScope(event.scope()), event.name(),
                    reportingStatus(event.status()), event.durationMillis());
            case REPORT_PUBLISHED -> reportPublished(Path.of(event.detailValue()));
        };
    }

    private static atf.reporting.console.AtfConsoleEvent.Scope reportingScope(AtfConsoleEvent.Scope scope) {
        return Enum.valueOf(atf.reporting.console.AtfConsoleEvent.Scope.class, scope.name());
    }

    private static atf.reporting.contract.AtfReportStatus reportingStatus(AtfReportStatus status) {
        return status == null ? null
                : Enum.valueOf(atf.reporting.contract.AtfReportStatus.class, status.name());
    }
}
