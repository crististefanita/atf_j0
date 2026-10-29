package atf.reporting.evidence.setup;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.evidence.markup.ReportEvidenceDisclosure;
import atf.reporting.markup.*;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportCheck;
import atf.reporting.model.ReportField;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.List;
import java.util.Locale;

/** One setup projection for both renderers; status is read before safe previews are bounded. */
public final class ReportSetupEvidenceMarkup {
    private ReportSetupEvidenceMarkup() { }
    public static boolean isSetup(List<ReportField> details) {
        return details.stream().anyMatch(field -> AtfReportEvidenceKeys.EVIDENCE_TYPE.equals(field.name())
                && AtfReportEvidenceKeys.REPORT_SETUP.equals(field.value()));
    }
    public static ReportEvidenceDisclosure disclosure(List<ReportField> details, SensitiveDataRedactor redactor, ReportingSettings settings) {
        var checks = details.stream().filter(field -> field.name().toLowerCase(Locale.ROOT)
                .startsWith(AtfReportEvidenceKeys.SETUP_CHECK_PREFIX)).map(field -> check(field.value())).toList();
        var rows = checks.stream().map(check -> List.of(preview(check.check(), redactor, settings),
                preview(check.expected(), redactor, settings), preview(check.actual(), redactor, settings),
                AtfReportStatus.orInfo(check.status()).resultMarker())).toList();
        var status = new ReportStatusAggregator(); checks.forEach(check -> status.record(check.status()));
        String body = AtfReportMarkup.tableHtml(AtfReportHtmlVocabulary.CssClass.ATF_SETUP_EVIDENCE_TABLE.value(),
                ReportEvidenceTable.of(List.of(AtfReportLabels.CHECK, AtfReportLabels.EXPECTED, AtfReportLabels.ACTUAL, AtfReportLabels.RESULT), rows));
        return ReportEvidenceDisclosure.withEvidenceStatus(AtfReportHtmlVocabulary.classList(
                AtfReportHtmlVocabulary.CssClass.SETUP_EVIDENCE, AtfReportHtmlVocabulary.CssClass.ATF_SETUP_EVIDENCE),
                AtfReportLabels.setupEvidence(checks.size()), body, AtfReportEvidenceKeys.REPORT_SETUP, status.resolve(null));
    }
    private static String preview(String text, SensitiveDataRedactor redactor, ReportingSettings settings) {
        return ExtentReportNodeSupport.display(redactor.redactText(text), settings);
    }
    private static ReportCheck check(String value) {
        String safe = value == null ? "" : value;
        String[] parts = safe.split(" \\| (?:expected=|actual=|result=)", -1);
        return parts.length == 4 ? new ReportCheck(parts[0], parts[1], parts[2], AtfReportStatus.fromDisplayName(parts[3]))
                : new ReportCheck(safe.isBlank() ? AtfReportLabels.SETUP_EVIDENCE : safe, "", "", AtfReportStatus.INFO);
    }
}
