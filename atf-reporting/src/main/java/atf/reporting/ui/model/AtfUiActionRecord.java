package atf.reporting.ui.model;

import atf.reporting.contract.AtfReportStatus;
import java.util.Objects;

/**
 * Immutable UI evidence independent of the consumer transport.
 * Expected/observed preserve captured payloads; only operation/target are trimmed.
 */
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
        expected = capturedValue(expected); observed = capturedValue(observed);
        if (durationMillis < 0) throw new IllegalArgumentException("durationMillis must not be negative");
        status = Objects.requireNonNull(status, "status"); source = Objects.requireNonNull(source, "source");
    }
    public String durationText() { return durationMillis + " ms"; }
    public String resultLabel() { return status.displayLabel(); }
    public String resultEmoji() { return status.marker(); }
    private static String required(String value, String name) {
        String normalized = capturedValue(value).trim();
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
    private static String capturedValue(String value) { return value == null ? "" : value; }
}
