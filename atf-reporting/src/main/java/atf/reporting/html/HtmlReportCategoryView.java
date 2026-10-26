package atf.reporting.html;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportExecutionModel;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/** Renders the fallback Category projection without owning semantic state. */
public final class HtmlReportCategoryView {
    String render(ReportExecutionModel snapshot) {
        var tags = new StringBuilder();
        snapshot.categories().tags().forEach(tag -> tags.append(
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                        escape(tag), AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TAG))));
        String body = HtmlReportFallbackViewSupport.heading(
                        AtfReportHtmlVocabulary.FallbackTab.CATEGORY)
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, tags.toString(),
                AtfReportHtmlVocabulary.classAttribute(AtfReportHtmlVocabulary.CssClass.TAG_LIST));
        return HtmlReportFallbackViewSupport.sectionStart(
                        AtfReportHtmlVocabulary.FallbackTab.CATEGORY)
                + body
                + HtmlReportFallbackViewSupport.sectionEnd();
    }
}
