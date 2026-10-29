package atf.reporting.api.evidence;

import atf.reporting.markup.AtfReportLabels;
import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.List;

/** Shared transport timing vocabulary for Extent and fallback. */
public final class ReportExecutionSummary {
    private ReportExecutionSummary() { }
    public static ReportEvidenceTable table(ReportExecutionEvidence execution, SensitiveDataRedactor redactor) {
        return table(execution, redactor, atf.reporting.configuration.ReportingSettings.defaults());
    }
    public static ReportEvidenceTable table(ReportExecutionEvidence execution, SensitiveDataRedactor redactor,
                                            atf.reporting.configuration.ReportingSettings settings) {
        var rows = new java.util.ArrayList<List<String>>();
        if (execution.response() == null) {
            rows.add(row("Terminal response availability", "No HTTP response received for a terminal exchange"));
        } else {
            rows.add(row(AtfReportLabels.RESPONSE_STATUS, execution.response().statusLabel()));
            rows.add(row(AtfReportLabels.RESPONSE_URL, redactor.redactText(execution.response().url())));
            rows.add(row(AtfReportLabels.RESPONSE_DURATION_MS, ReportTransportTiming.display(execution.response().duration())));
        }
        rows.add(row(AtfReportLabels.TOTAL_DURATION_MS, ReportTransportTiming.display(execution.totalDuration())));
        rows.add(row(AtfReportLabels.ATTEMPTS, String.valueOf(execution.attempts())));
        rows.add(row(AtfReportLabels.RETRY_EVENTS, String.valueOf(execution.retryEvents().size())));
        execution.termination().ifPresent(value -> {
            rows.add(row("Execution termination", value.outcome()));
            rows.add(row("Execution failure", atf.reporting.markup.ExtentReportNodeSupport.display(
                    redactor.redactText(value.diagnostic()), settings)));
        });
        rows.add(List.of("TIMING SCOPE", "Response and total: transport measurements; node badge: reporting lifecycle"));
        return ReportEvidenceTable.of(List.of(AtfReportLabels.FIELD, AtfReportLabels.VALUE), rows);
    }

    private static List<String> row(String key, String value) {
        return List.of(EvidenceTableFormatter.displayName(key), value);
    }
}
