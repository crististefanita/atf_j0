package atf.reporting.html;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.Locale;

/** Small, renderer-neutral HTML helpers shared by fallback report views. */
public final class HtmlReportMarkupSupport {
    private HtmlReportMarkupSupport() {
    }

    public static String escape(String value) {
        return AtfReportHtmlVocabulary.escapeHtml(value);
    }

    public static String phaseKey(String name) {
        return (name == null ? "" : name.trim()).toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }
}
