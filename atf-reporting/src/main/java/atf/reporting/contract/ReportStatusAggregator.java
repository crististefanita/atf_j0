package atf.reporting.contract;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Single status aggregation policy used by all standalone renderers. */
public final class ReportStatusAggregator {
    private final EnumMap<AtfReportStatus, Integer> counts = new EnumMap<>(AtfReportStatus.class);
    private AtfReportStatus aggregate = AtfReportStatus.INFO;

    public ReportStatusAggregator() { for (var status : AtfReportStatus.values()) counts.put(status, 0); }
    public void record(AtfReportStatus status) {
        Objects.requireNonNull(status, "status");
        counts.compute(status, (ignored, count) -> count + 1);
        aggregate = AtfReportStatus.worst(aggregate, status);
    }
    public AtfReportStatus aggregate() { return aggregate; }
    public int count(AtfReportStatus status) { return counts.getOrDefault(Objects.requireNonNull(status), 0); }
    public Map<AtfReportStatus, Integer> counts() { return Map.copyOf(counts); }
    public boolean hasWarnings() { return count(AtfReportStatus.WARNING) > 0; }
}
