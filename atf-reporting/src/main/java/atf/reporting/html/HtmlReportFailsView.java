package atf.reporting.html;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.security.SensitiveDataRedactor;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/** Renders failed contract assertions and explicit execution diagnostics from one projection. */
public final class HtmlReportFailsView {
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    String render(ReportExecutionModel snapshot) {
        var html = new StringBuilder();
        html.append(HtmlReportFallbackViewSupport.sectionStart(
                        AtfReportHtmlVocabulary.FallbackTab.FAILS))
                .append(HtmlReportFallbackViewSupport.heading(
                        AtfReportHtmlVocabulary.FallbackTab.FAILS))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.FAIL_LIST)));
        for (var failure : snapshot.failures().items()) {
            String status = ReportEvidenceMarkup.statusBadge(AtfReportStatus.FAIL);
            String content = status
                    + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STRONG,
                    escape(failure.assertionName()))
                    + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                    escape(redactor.redactField(failure.assertionName(), failure.actual())));
            html.append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                    content,
                    AtfReportHtmlVocabulary.classAttribute(
                            AtfReportHtmlVocabulary.CssClass.FAIL_ROW),
                    AtfReportHtmlVocabulary.attribute(
                            AtfReportHtmlVocabulary.Attribute.HREF,
                            "#" + failure.scenarioId())));
        }
        if (snapshot.failures().isEmpty()) {
            html.append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.P,
                    AtfReportLabels.NO_FAILED_ASSERTIONS,
                    AtfReportHtmlVocabulary.classAttribute(
                            AtfReportHtmlVocabulary.CssClass.EMPTY)));
        }
        return html.append(AtfReportHtmlVocabulary.closeTag(
                        AtfReportHtmlVocabulary.Tag.DIV))
                .append(HtmlReportFallbackViewSupport.sectionEnd())
                .toString();
    }
}
