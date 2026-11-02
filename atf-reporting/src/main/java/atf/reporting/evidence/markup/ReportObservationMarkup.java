package atf.reporting.evidence.markup;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportJsonBodyFormatter;
import atf.reporting.model.ReportObservation;
import atf.reporting.security.SensitiveDataRedactor;

import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.*;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.*;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.*;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.*;

/** One disclosure policy shared by both renderers; all values are redacted before preview/escaping. */
public final class ReportObservationMarkup {
    private ReportObservationMarkup() { }

    public static String render(ReportObservation value, ReportingSettings settings, SensitiveDataRedactor redactor) {
        var html = new StringBuilder(openTag(DIV, attribute(NODE_ROLE, "observation")));
        html.append(ReportEvidenceMarkup.disclosureStart(OBSERVATION_EVIDENCE.value(),
                preview(redactor.redactText(value.title()), settings)));
        var fields = value.fields().stream().map(field -> new ReportField(
                preview(redactor.redactText(field.name()), settings),
                preview(redactor.redactField(field.name(), field.value()), settings))).toList();
        if (!fields.isEmpty()) html.append(AtfReportMarkup.tableHtml(HELM_EVIDENCE_TABLE.value(), EvidenceTableFormatter.toTable(fields)));
        for (var body : value.bodies()) {
            String safe = redactor.redactField(body.label(), body.text());
            if (body.format() == ReportObservation.Format.JSON) safe = ReportJsonBodyFormatter.format(redactor.redactJson(safe));
            html.append(ReportEvidenceMarkup.disclosureStart(OBSERVATION_EVIDENCE.value(), preview(redactor.redactText(body.label()), settings)))
                    .append(element(PRE, ReportEvidenceMarkup.preformattedText(ExtentReportNodeSupport.displayBody(safe, settings)),
                            classAttribute(JSON_CODE, OBSERVATION_BODY), attribute(TABINDEX, "0")))
                    .append(ReportEvidenceMarkup.disclosureEnd());
        }
        for (var artifact : value.artifacts()) {
            String raw = artifact.location().toASCIIString();
            String safe = redactor.redactField(artifact.label(), raw);
            String label = preview(redactor.redactText(artifact.label()), settings);
            html.append(ReportEvidenceMarkup.disclosureStart(OBSERVATION_EVIDENCE.value(), label));
            // A masked location must never survive in a hidden href or tooltip.
            if (safe.equals(raw)) html.append(element(A, escapeHtml("Open artifact reference"),
                    attribute(HREF, raw), attribute(REL, "noopener noreferrer")));
            html.append(element(PRE, ReportEvidenceMarkup.preformattedText(ExtentReportNodeSupport.displayBody(safe, settings)),
                    classAttribute(JSON_CODE, OBSERVATION_BODY), attribute(TABINDEX, "0")));
            if (!artifact.sha256().isEmpty()) html.append(element(P, escapeHtml("SHA-256: " + artifact.sha256())));
            html.append(ReportEvidenceMarkup.disclosureEnd());
        }
        return html.append(ReportEvidenceMarkup.disclosureEnd()).append(closeTag(DIV)).toString();
    }
    private static String preview(String value, ReportingSettings settings) {
        return ExtentReportNodeSupport.display(value, settings);
    }
}
