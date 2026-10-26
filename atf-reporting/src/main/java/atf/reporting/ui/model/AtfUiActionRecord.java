package atf.reporting.ui.model;

import atf.reporting.contract.AtfReportStatus;
import java.util.Objects;

/** Immutable Playwright/audit evidence row owned by reporting. */
public record AtfUiActionRecord(int sequence, String operation, String target, String expected,
                                String observed, long durationMillis, AtfReportStatus status,
                                AtfUiEvidenceSource source) {
    public AtfUiActionRecord(int sequence, String operation, String target, String expected,
                             String observed, long durationMillis, AtfReportStatus status) {
        this(sequence, operation, target, expected, observed, durationMillis, status, AtfUiEvidenceSource.ACTION);
    }
    public AtfUiActionRecord {
        if (sequence < 1) throw new IllegalArgumentException("sequence must be positive");
        operation = required(operation, "operation"); target = required(target, "target");
        expected = normalize(expected); observed = normalize(observed);
        if (durationMillis < 0) throw new IllegalArgumentException("durationMillis must not be negative");
        status = Objects.requireNonNull(status, "status"); source = Objects.requireNonNull(source, "source");
    }
    public String durationText() { return durationMillis + " ms"; }
    public String resultLabel() { return status.displayLabel(); }
    public String resultEmoji() { return status.marker(); }
    private static String required(String value, String name) { String v=normalize(value); if(v.isBlank()) throw new IllegalArgumentException(name+" must not be blank"); return v; }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
}
