package atf.api.reporting.evidence.markup;

import atf.api.reporting.evidence.EvidenceTableFormatter;
import atf.api.reporting.markup.AtfReportMarkup;
import atf.api.reporting.markup.AtfReportEvidenceKeys;
import atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.api.reporting.model.ReportField;
import atf.api.reporting.security.SensitiveDataRedactor;

import java.util.List;

/** One typed, renderer-neutral disclosure for compact UI flow provenance. */
public final class UiFlowProvenanceMarkup {
    private UiFlowProvenanceMarkup() { }

    public static String render(List<ReportField> details, SensitiveDataRedactor redactor) {
        List<ReportField> fields = EvidenceTableFormatter.uiFlowProvenanceFields(details);
        if (fields.isEmpty()) return "";
        long checkpoints = fields.stream().filter(field ->
                field.name().startsWith(AtfReportEvidenceKeys.FLOW_CHECKPOINT_PREFIX)
                        && field.name().endsWith(".phase")).count();
        List<ReportField> safe = fields.stream().map(field -> new ReportField(
                field.name().replace("phaseStatus", "phase status"),
                redactor.redactField(field.name(), field.value()))).toList();
        String table = AtfReportMarkup.tableHtml(
                AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE.value(),
                EvidenceTableFormatter.toTable(safe));
        return ReportEvidenceMarkup.disclosure("atf-ui-flow-provenance",
                "UI flow provenance · " + checkpoints + (checkpoints == 1 ? " checkpoint" : " checkpoints"),
                table);
    }
}
