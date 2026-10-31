package atf.reporting.evidence.markup;

import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportField;
import atf.reporting.security.SensitiveDataRedactor;

import java.util.List;

/** Typed, renderer-neutral context for phase-local and cross-phase UI flows. */
public final class UiFlowProvenanceMarkup {
    private UiFlowProvenanceMarkup() { }

    public static String render(List<ReportField> details, SensitiveDataRedactor redactor) {
        List<ReportField> fields = EvidenceTableFormatter.uiFlowProvenanceFields(details);
        if (fields.isEmpty()) return "";
        int checkpoints = (int) fields.stream().map(field -> field.name().toLowerCase(java.util.Locale.ROOT))
                .filter(name -> name.startsWith(AtfReportEvidenceKeys.FLOW_CHECKPOINT_PREFIX)
                        && name.endsWith(".phase")).count();
        List<ReportField> safe = fields.stream().map(field -> new ReportField(
                field.name().replace("phaseStatus", "phase status"),
                redactor.redactField(field.name(), field.value()))).toList();
        String table = AtfReportMarkup.tableHtml(
                AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE.value(),
                EvidenceTableFormatter.toTable(safe));
        String label = checkpoints > 0 ? AtfReportLabels.uiFlowProvenance(checkpoints)
                : AtfReportLabels.uiFlowContext(fields.size());
        return ReportEvidenceMarkup.disclosure("atf-ui-flow-provenance", label, table);
    }
}
