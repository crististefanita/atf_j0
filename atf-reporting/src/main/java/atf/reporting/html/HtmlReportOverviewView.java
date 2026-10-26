package atf.reporting.html;
import atf.reporting.markup.AtfReportLabels;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportExecutionModel;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/** Renders only the fallback Overview aggregate. */
public final class HtmlReportOverviewView {
    String render(ReportExecutionModel snapshot) {
        var overview = snapshot.overview();
        String summary = metric("Tests", overview.tests()) + metric("Passed", overview.passed())
                + metric("Failed", overview.failed()) + metric("Skipped", overview.skipped());
        String body = HtmlReportFallbackViewSupport.heading(
                        AtfReportHtmlVocabulary.FallbackTab.OVERVIEW)
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, summary,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.SUMMARY))
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.P,
                AtfReportLabels.OVERVIEW_DEFAULT_COPY,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.CONTRACT));
        return HtmlReportFallbackViewSupport.sectionStart(
                        AtfReportHtmlVocabulary.FallbackTab.OVERVIEW)
                + body
                + HtmlReportFallbackViewSupport.sectionEnd();
    }

    private static String metric(String label, long value) {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, escape(label))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STRONG,
                        Long.toString(value)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.METRIC));
    }
}
