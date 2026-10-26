package atf.ui.tests.extent.support.inspection.duration;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.ui.contract.ReportUiContract;
import atf.testkit.files.AtfTestResourceReader;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads duration markers from the four report projections without owning navigation or policy. */
public final class ApiReportDurationProjectionAuditor {
    private static final String INSPECTION = AtfTestResourceReader.readResourceUtf8(
            ApiReportDurationProjectionAuditor.class, "/atf/reporting/inspection/duration-projections.js");
    private final UiAuditFlow auditFlow;

    public ApiReportDurationProjectionAuditor(UiAuditFlow auditFlow) {
        this.auditFlow = auditFlow;
    }

    public List<ReportAuditSnapshot.DurationProjection> inspect(Page page) {
        for (ReportUiContract.Tab tab : ReportUiContract.Tab.values()) {
            auditFlow.openTab(tab.hash());
        }
        auditFlow.openTab(ReportUiContract.Tab.TESTS.hash());
        return inspectCurrentDom(page);
    }

    /** Reusable read-only DOM query for component probes and navigation adapters. */
    @SuppressWarnings("unchecked")
    public static List<ReportAuditSnapshot.DurationProjection> inspectCurrentDom(Page page) {
        Map<String, Object> raw = (Map<String, Object>) page.evaluate(INSPECTION);
        List<ReportAuditSnapshot.DurationProjection> result = new ArrayList<>();
        raw.forEach((name, value) -> {
            Map<String, Object> projection = (Map<String, Object>) value;
            result.add(new ReportAuditSnapshot.DurationProjection(name,
                    ReportAuditChecks.count(projection.get("fieldCount")),
                    ReportAuditChecks.count(projection.get("validCount")),
                    (List<String>) projection.getOrDefault("violations", List.of())));
        });
        return List.copyOf(result);
    }
}
