package atf.api.reporting.evidence.markup;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.reporting.security.SensitiveDataRedactor;

import atf.api.reporting.contract.AtfReportStatus;
import atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.ARIA_EXPANDED;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.ARIA_HIDDEN;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.EVIDENCE_STATUS;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.EVIDENCE_TYPE;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.AttributeValue.FALSE;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.DISCLOSURE_GLYPH;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.SE_TITLE;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.STEP_DETAIL;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.DETAILS;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.SPAN;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.SUMMARY;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.attribute;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.classAttribute;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.closeTag;
import static atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary.openTag;

/** Shared semantic primitives for Extent and fallback evidence renderers. */
public final class ReportEvidenceMarkup {
    private ReportEvidenceMarkup() {
    }

    /** Machine-readable facts from the captured exchange, independent of display titles. */
    public static String executionAttributes(ReportExecutionEvidence execution,
                                             SensitiveDataRedactor redactor) {
        var request = execution.request();
        var response = execution.response();
        var attributes = new java.util.ArrayList<String>();
        if (request != null) {
            attributes.add(attribute(AtfReportHtmlVocabulary.Attribute.REQUEST_METHOD, request.method()));
            attributes.add(attribute(AtfReportHtmlVocabulary.Attribute.REQUEST_PATH,
                    redactor.redactText(request.path())));
            attributes.add(attribute(AtfReportHtmlVocabulary.Attribute.REQUEST_BODY_AVAILABILITY,
                    request.bodyAvailability().name()));
        }
        if (response != null) {
            attributes.add(attribute(AtfReportHtmlVocabulary.Attribute.RESPONSE_BODY_AVAILABILITY,
                    response.bodyAvailability().name()));
        }
        return String.join(" ", attributes);
    }

    public static String disclosureStart(String kind, String title) {
        return disclosureStart(ReportEvidenceDisclosure.standard(kind, title, ""));
    }

    public static String disclosureStart(ReportEvidenceDisclosure disclosure) {
        String rawTitle = disclosure.title();
        String safeTitle = AtfReportHtmlVocabulary.escapeHtml(rawTitle);
        var attributes = new java.util.ArrayList<String>();
        attributes.add(classAttribute(STEP_DETAIL.value(), disclosure.kind()));
        if (disclosure.evidenceType() != null) {
            attributes.add(attribute(EVIDENCE_TYPE, disclosure.evidenceType()));
        }
        if (disclosure.status() != null) {
            attributes.add(attribute(EVIDENCE_STATUS,
                    AtfReportHtmlVocabulary.statusValue(disclosure.status())));
        }
        return openTag(DETAILS, attributes.toArray(String[]::new))
                + openTag(SUMMARY, attribute(ARIA_EXPANDED, FALSE))
                + openTag(SPAN, classAttribute(DISCLOSURE_GLYPH),
                attribute(ARIA_HIDDEN, AtfReportHtmlVocabulary.AttributeValue.TRUE))
                + closeTag(SPAN)
                + openTag(SPAN, classAttribute(SE_TITLE),
                attribute(AtfReportHtmlVocabulary.Attribute.TITLE, rawTitle),
                attribute(AtfReportHtmlVocabulary.Attribute.ARIA_LABEL, rawTitle))
                + safeTitle
                + closeTag(SPAN)
                + closeTag(SUMMARY)
                + openTag(AtfReportHtmlVocabulary.Tag.DIV,
                classAttribute(AtfReportHtmlVocabulary.CssClass.BODY));
    }

    public static String disclosureEnd() {
        return closeTag(AtfReportHtmlVocabulary.Tag.DIV) + closeTag(DETAILS);
    }

    public static String disclosure(String kind, String title, String body) {
        return disclosure(ReportEvidenceDisclosure.standard(kind, title, body));
    }

    public static String disclosure(ReportEvidenceDisclosure disclosure) {
        return disclosureStart(disclosure) + disclosure.body() + disclosureEnd();
    }

    /** Canonical status badge shared by fallback evidence views. */
    public static String statusBadge(AtfReportStatus status) {
        AtfReportStatus safeStatus = AtfReportStatus.orInfo(status);
        return openTag(SPAN, classAttribute(
                        AtfReportHtmlVocabulary.statusClasses(safeStatus)))
                + AtfReportHtmlVocabulary.statusLabel(safeStatus)
                + closeTag(SPAN);
    }
}
