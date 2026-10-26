package atf.reporting.html;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

/** Shared shell primitives for renderer-neutral fallback views. */
final class HtmlReportFallbackViewSupport {
    private HtmlReportFallbackViewSupport() {
    }

    static String sectionStart(AtfReportHtmlVocabulary.FallbackTab tab) {
        return AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SECTION,
                AtfReportHtmlVocabulary.idAttribute(tab.value()),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TAB_PANEL));
    }

    static String heading(AtfReportHtmlVocabulary.FallbackTab tab) {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.H2, tab.label());
    }

    static String sectionEnd() {
        return AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SECTION);
    }
}
