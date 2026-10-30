package atf.reporting.evidence.diagnostic;

import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;

import java.util.List;

import static atf.reporting.markup.AtfReportLabels.EXECUTION_DIAGNOSTICS;
import static atf.reporting.markup.AtfReportLabels.FIELD;
import static atf.reporting.markup.AtfReportLabels.VALUE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.PHASE_NAME;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.DIAGNOSTIC_STATUS;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.ASSERTION_PREVIEW_LIMIT;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.PHASE_DIAGNOSTICS;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.DIV;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.attribute;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.closeTag;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.openTag;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.statusValue;

/** Shared scenario-owned diagnostic projection, never an HTTP response or a contract assertion. */
public final class ReportPhaseDiagnosticMarkup {
    private ReportPhaseDiagnosticMarkup() { }

    public static String render(List<ReportPhaseDiagnostic> diagnostics, SensitiveDataRedactor redactor) {
        return render(diagnostics, redactor, atf.reporting.configuration.ReportingSettings.DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS);
    }

    public static String render(List<ReportPhaseDiagnostic> diagnostics, SensitiveDataRedactor redactor, int previewLimit) {
        if (previewLimit <= 0) throw new IllegalArgumentException("diagnostic preview limit must be positive");
        if (diagnostics.isEmpty()) return "";
        String phase = diagnostics.getFirst().phase();
        List<List<String>> rows = diagnostics.stream()
                .map(diagnostic -> List.of(diagnostic.kind().label(),
                        redactor.redactField(diagnostic.kind().label(), diagnostic.message())))
                .toList();
        String table = AtfReportMarkup.tableHtml(HELM_EVIDENCE_TABLE.value(),
                ReportEvidenceTable.of(List.of(FIELD, VALUE), rows));
        String body = openTag(DIV, attribute(PHASE_NAME, phase),
                attribute(ASSERTION_PREVIEW_LIMIT, Integer.toString(previewLimit)),
                attribute(DIAGNOSTIC_STATUS, statusValue(diagnostics.getFirst().status()))) + table + closeTag(DIV);
        return ReportEvidenceMarkup.disclosure(PHASE_DIAGNOSTICS.value(),
                String.format("%s — %s", EXECUTION_DIAGNOSTICS, phase), body);
    }
}
