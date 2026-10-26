package atf.ui.tests.extent.support.inspection.duration;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads duration markers from the four report projections without owning navigation or policy. */
public final class ApiReportDurationProjectionAuditor {
    private final UiAuditFlow auditFlow;

    public ApiReportDurationProjectionAuditor(UiAuditFlow auditFlow) {
        this.auditFlow = auditFlow;
    }

    @SuppressWarnings("unchecked")
    public List<ReportAuditSnapshot.DurationProjection> inspect(Page page) {
        for (ReportUiContract.Tab tab : ReportUiContract.Tab.values()) {
            auditFlow.openTab(tab.hash());
        }
        auditFlow.openTab(ReportUiContract.Tab.TESTS.hash());
        Map<String, Object> raw = (Map<String, Object>) page.evaluate("""
                () => {
                  const selectors = {
                    Tests: '#test-view [data-duration-source],#test-view [data-duration-ms]',
                    Overview: '#dashboard-view [data-duration-source],#dashboard-view [data-duration-ms]',
                    Categories: '#category-view [data-duration-source],#category-view [data-duration-ms]',
                    Fails: '#atf-fails-view [data-duration-source],#atf-fails-view [data-duration-ms]'
                  };
                  const result = {};
                  Object.entries(selectors).forEach(([name, selector]) => {
                    const fields = [...document.querySelectorAll(selector)];
                    const violations = [];
                    let valid = 0;
                    fields.forEach(field => {
                      const source = field.getAttribute('data-duration-source') || '';
                      const text = String(field.textContent || '').replace(/\\s+/g, ' ').trim();
                      const marker = field.getAttribute('data-duration-ms');
                      const numericMarker = marker !== null
                        && /^\\d+(?:\\.\\d+)?$/.test(marker);
                      const formattedText = /^(?:\\d+(?:\\.\\d+)? ms|\\d+(?:\\.\\d+)? s)$/.test(text);
                      const validMarker = source === 'absent'
                        ? marker === null && (text === '—'
                          || /^(?:pass|fail|warning|skip)$/i.test(text))
                        : numericMarker && (source === 'aggregated'
                          || source === 'measured'
                          || formattedText);
                      if (validMarker) valid++;
                      else violations.push(name + ': ' + text);
                    });
                    result[name] = {fieldCount: fields.length, validCount: valid, violations};
                  });
                  return result;
                }
                """);
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
