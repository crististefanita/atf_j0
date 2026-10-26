package atf.reporting.html;

import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.Objects;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/**
 * Semantic shell for the renderer-neutral fallback report.
 *
 * <p>The session composes the document; this class owns only the HTML shell so
 * tag/attribute structure cannot drift into another collection of raw string
 * constants. The returned markup remains compact because asset compaction is a
 * publication concern, not a readability concern in this Java source.</p>
 */
final class HtmlReportShell {
    private HtmlReportShell() {
    }

    static String open(String css) {
        return "<!doctype html>"
                + AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.HTML,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.LANG, "en"))
                + AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.HEAD)
                + AtfReportHtmlVocabulary.voidElement(AtfReportHtmlVocabulary.Tag.LINK,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.REL, "icon"),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.HREF, "data:,"))
                + AtfReportHtmlVocabulary.voidElement(AtfReportHtmlVocabulary.Tag.META,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.CHARSET, "utf-8"))
                + AtfReportHtmlVocabulary.voidElement(AtfReportHtmlVocabulary.Tag.META,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.NAME, "viewport"),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.CONTENT,
                        "width=device-width,initial-scale=1"))
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TITLE,
                escape(AtfReportLabels.REPORT_TITLE))
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STYLE,
                Objects.requireNonNullElse(css, ""))
                + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.HEAD)
                + AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.BODY)
                + header();
    }

    static String header() {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.HEADER,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, "ATF",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.BRAND))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                        escape(AtfReportLabels.REPORT_TITLE),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TITLE)));
    }

    static String mainAfterNavigation() {
        return AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.NAV)
                + AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.MAIN);
    }

    static String close(String script) {
        return AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.MAIN)
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SCRIPT,
                Objects.requireNonNullElse(script, ""))
                + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.BODY)
                + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.HTML);
    }
}
