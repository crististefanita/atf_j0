package atf.core.reporting.console;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.Objects;
import java.nio.file.Path;

/**
 * Renderer-neutral console event emitted by an ATF reporting lifecycle.
 *
 * <p>The event is deliberately smaller than an HTML/report model. It carries
 * only the information needed by a console sink and never exposes a vendor
 * reporter or a mutable execution context.</p>
 */
public record AtfConsoleEvent(Type type, Scope scope, String name,
                              AtfReportStatus status, long durationMillis,
                              String detailName, String detailValue) {
    public enum Type { START, DETAIL, CODE, COMPLETE, REPORT_PUBLISHED }

    public enum Scope { SCENARIO, PHASE, REQUEST, VALIDATION, UI_ACTION, REPORT }

    public AtfConsoleEvent {
        type = Objects.requireNonNull(type, "type");
        scope = Objects.requireNonNull(scope, "scope");
        name = name == null ? "" : name.trim();
        if (durationMillis < 0) {
            throw new IllegalArgumentException("durationMillis must not be negative");
        }
        if (type == Type.COMPLETE) {
            status = Objects.requireNonNull(status, "status");
        }
        if (type == Type.REPORT_PUBLISHED
                && (scope != Scope.REPORT || !Path.of(detailValue == null ? "" : detailValue).isAbsolute())) {
            throw new IllegalArgumentException("published report event requires an absolute report path");
        }
        detailName = detailName == null ? "" : detailName.trim();
        detailValue = detailValue == null ? "" : detailValue;
    }

    /**
     * Copies a compatible external event vocabulary by enum name, without a dependency
     * on its owner. Unknown names and invalid event data fail through the Core contract.
     */
    public static AtfConsoleEvent fromExternalEnums(Enum<?> type, Enum<?> scope, String name,
                                                    Enum<?> status, long durationMillis,
                                                    String detailName, String detailValue) {
        return new AtfConsoleEvent(
                Type.valueOf(Objects.requireNonNull(type, "type").name()),
                Scope.valueOf(Objects.requireNonNull(scope, "scope").name()),
                name, status == null ? null : AtfReportStatus.valueOf(status.name()),
                durationMillis, detailName, detailValue);
    }

    public static AtfConsoleEvent started(Scope scope, String name) {
        return new AtfConsoleEvent(Type.START, scope, name, null, 0, "", "");
    }

    public static AtfConsoleEvent detail(Scope scope, String name, String value) {
        return new AtfConsoleEvent(Type.DETAIL, scope, "", null, 0, name, value);
    }

    public static AtfConsoleEvent code(Scope scope, String name, String value) {
        return new AtfConsoleEvent(Type.CODE, scope, "", null, 0, name, value);
    }

    public static AtfConsoleEvent completed(Scope scope, String name,
                                            AtfReportStatus status, long durationMillis) {
        return new AtfConsoleEvent(Type.COMPLETE, scope, name, status, durationMillis, "", "");
    }

    /** Creates the terminal notification for an already-published report artifact. */
    public static AtfConsoleEvent reportPublished(Path reportPath) {
        Path normalized = Objects.requireNonNull(reportPath, "reportPath").toAbsolutePath().normalize();
        return new AtfConsoleEvent(Type.REPORT_PUBLISHED, Scope.REPORT, "Extent report",
                null, 0, "path", normalized.toString());
    }
}
