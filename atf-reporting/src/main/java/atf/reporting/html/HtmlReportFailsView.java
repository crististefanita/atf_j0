package atf.reporting.html;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.markup.ExtentReportNodeSupport;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/** Renders failed contract assertions and explicit execution diagnostics from one projection. */
public final class HtmlReportFailsView {
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    private final ReportingSettings settings;

    public HtmlReportFailsView() { this(ReportingSettings.defaults()); }

    public HtmlReportFailsView(ReportingSettings settings) {
        this.settings = java.util.Objects.requireNonNull(settings, "settings");
    }
    String render(ReportExecutionModel snapshot) {
        var html = new StringBuilder();
        html.append(HtmlReportFallbackViewSupport.sectionStart(
                        AtfReportHtmlVocabulary.FallbackTab.FAILS))
                .append(HtmlReportFallbackViewSupport.heading(
                        AtfReportHtmlVocabulary.FallbackTab.FAILS))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.FAIL_LIST)));
        var ordinals = new java.util.HashMap<String, Integer>();
        for (var failure : snapshot.failures().items()) {
            int ordinal = ordinals.merge(failure.scenarioId(), 1, Integer::sum);
            String status = ReportEvidenceMarkup.statusBadge(AtfReportStatus.FAIL);
            String content = status
                    + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STRONG,
                    escape(redactor.redactText(failure.assertionName())))
                    + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                    escape(ExtentReportNodeSupport.display(
                            redactor.redactField(failure.assertionName(), failure.actual()), settings)));
            html.append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                    content,
                    AtfReportHtmlVocabulary.classAttribute(
                            AtfReportHtmlVocabulary.CssClass.FAIL_ROW),
                    AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.FAILURE_ORDINAL, Integer.toString(ordinal)),
                    AtfReportHtmlVocabulary.attribute(
                            AtfReportHtmlVocabulary.Attribute.HREF,
                            "#" + failure.scenarioId() + "&failure-ordinal=" + ordinal)));
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
