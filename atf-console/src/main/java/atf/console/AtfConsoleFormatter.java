package atf.console;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.duration.AtfDurationFormatter;

import java.util.ArrayList;
import java.util.List;

/** Pure renderer for one console event; it performs no I/O or state changes. */
final class AtfConsoleFormatter {
    private final ConsoleOutputStyle style;

    AtfConsoleFormatter(ConsoleOutputStyle style) {
        this.style = style;
    }

    List<String> format(AtfConsoleEvent event, boolean detailsEnabled) {
        return switch (event.type()) {
            case START -> List.of(style.startMarker() + displayName(event.name()));
            case DETAIL -> detailsEnabled ? formatDetail(event) : List.of();
            case CODE -> detailsEnabled ? formatCode(event) : List.of();
            case COMPLETE -> List.of(formatCompletion(event));
            case REPORT_PUBLISHED -> List.of("Extent report: " + event.detailValue());
        };
    }

    private List<String> formatDetail(AtfConsoleEvent event) {
        String label = event.detailName().isBlank() ? "detail" : event.detailName();
        return List.of(style.detailMarker() + label + "=" + singleLine(event.detailValue()));
    }

    private List<String> formatCode(AtfConsoleEvent event) {
        String label = event.detailName().isBlank() ? "body" : event.detailName();
        String value = event.detailValue();
        if (value == null || value.isBlank()) {
            return List.of(style.detailMarker() + label + "=<empty>");
        }

        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.contains("\n")) {
            return List.of(style.detailMarker() + label + "=" + normalized);
        }

        List<String> lines = new ArrayList<>();
        lines.add(style.detailMarker() + label);
        for (String row : normalized.split("\n", -1)) {
            lines.add("  " + row);
        }
        return List.copyOf(lines);
    }

    private String formatCompletion(AtfConsoleEvent event) {
        String icon = style.statusIcon(event.status());
        String prefix = icon.isBlank() ? "" : icon + " ";
        return prefix + event.status().name() + ": "
                + completionMessage(event) + " (" + AtfDurationFormatter.formatMillis(event.durationMillis()) + ")";
    }

    private static String displayName(String name) {
        return name.replace('\u2014', '-').replace(String.valueOf('\u2192'), "->");
    }

    private static String completionMessage(AtfConsoleEvent event) {
        return switch (event.scope()) {
            case REQUEST -> switch (event.status()) {
                case PASS, WARNING -> "Call completed";
                case FAIL -> "Call failed";
                case SKIP -> "Call skipped";
                case INFO -> "Call finished";
            };
            case VALIDATION -> switch (event.status()) {
                case PASS -> "Validated";
                case WARNING -> "Validated with warnings";
                case FAIL -> "Validation failed";
                case SKIP -> "Validation skipped";
                case INFO -> "Validation finished";
            };
            case SCENARIO, PHASE -> displayName(event.name());
            case REPORT -> displayName(event.name());
            case UI_ACTION -> switch (event.status()) {
                case PASS, WARNING -> "UI action completed";
                case FAIL -> "UI action failed";
                case SKIP -> "UI action skipped";
                case INFO -> "UI action finished";
            };
        };
    }

    private static String singleLine(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }
}
