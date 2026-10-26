package atf.reporting.contract;

import java.util.Locale;

/** Renderer-neutral status vocabulary owned by the standalone reporting module. */
public enum AtfReportStatus {
    INFO(0, "ℹ️"),
    PASS(1, "✅"),
    WARNING(2, "⚠️"),
    SKIP(3, "⏭️"),
    FAIL(4, "❌");

    private final int severity;
    private final String marker;

    AtfReportStatus(int severity, String marker) {
        this.severity = severity;
        this.marker = marker;
    }

    public int severity() { return severity; }

    /** Stable inline marker used by Extent evidence rows and UI assertions. */
    public String marker() { return marker; }

    /** Canonical compact result used in evidence cells. */
    public String resultMarker() { return marker + " " + name(); }

    /** Null-safe boundary for legacy adapters that may omit a status. */
    public static AtfReportStatus orInfo(AtfReportStatus status) {
        return status == null ? INFO : status;
    }

    public String displayValue() { return name().toLowerCase(Locale.ROOT); }

    public String displayLabel() {
        String value = displayValue();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static AtfReportStatus worst(AtfReportStatus left, AtfReportStatus right) {
        return left.severity >= right.severity ? left : right;
    }

    public static AtfReportStatus fromDisplayName(String label) {
        if (label == null || label.isBlank()) return INFO;
        return switch (label.trim().toUpperCase(Locale.ROOT)) {
            case "PASS" -> PASS;
            case "WARNING", "WARN" -> WARNING;
            case "SKIP", "SKIPPED" -> SKIP;
            case "FAIL", "FAILED", "ERROR" -> FAIL;
            default -> INFO;
        };
    }
}
