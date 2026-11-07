package atf.reporting.model.projection;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.util.Arrays;
import java.util.List;

/** Captured UI failures in the same ACTION/OBSERVE order as their rendered source tables. */
final class ReportUiFailureProjection {
    private ReportUiFailureProjection() { }

    static List<ReportExecutionModel.Failure> collect(String scenarioId, List<AtfUiActionRecord> records,
                                                       ReportUiActionDetailsMode detailsMode) {
        return Arrays.stream(AtfUiEvidenceSource.values())
                .filter(source -> source == AtfUiEvidenceSource.OBSERVE || detailsMode != ReportUiActionDetailsMode.SUMMARY_ONLY)
                .flatMap(source -> records.stream()
                        .filter(row -> row.source() == source && row.status() == AtfReportStatus.FAIL))
                .map(row -> new ReportExecutionModel.Failure(scenarioId, label(row), row.observed())).toList();
    }

    private static String label(AtfUiActionRecord row) {
        String type = row.source() == AtfUiEvidenceSource.ACTION ? "UI action" : "Audit observation";
        return type + " · " + row.operation() + " #" + row.sequence() + " → " + row.target();
    }
}
