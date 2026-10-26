package atf.reporting.markup;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the secondary, collapsed-by-default overview audit tables. */
public final class ExtentReportOverviewMarkup {
    private static final Pattern CATEGORY_CONTAINER = Pattern.compile(
            "<div\\b(?=[^>]*\\bclass\\s*=\\s*\"[^\"]*\\bcategory-container\\b)[^>]*>");

    private enum AuditPanel {
        SCENARIOS("ATF scenario identities by test class", "atf-audit-scenarios", "classes", "NAME|COUNT|SHARE"),
        PACKAGES("Test packages", "atf-audit-packages", "packages", "NAME|COUNT|SHARE"),
        TAGS("Other annotations / tags", "atf-audit-tags", "other", "NAME|COUNT");

        private final String title;
        private final String id;
        private final String suffix;
        private final String headers;

        AuditPanel(String title, String id, String suffix, String headers) {
            this.title = title;
            this.id = id;
            this.suffix = suffix;
            this.headers = headers;
        }
    }

    private ExtentReportOverviewMarkup() {
    }

    public static String inject(String html) {
        if (html == null || html.isBlank()) return html;
        String metadataId = AtfReportHtmlVocabulary.idAttribute(
                AtfReportHtmlVocabulary.ElementId.EXECUTION_METADATA);
        if (html.contains(metadataId)) return html;

        String metadata = details(AtfReportHtmlVocabulary.ElementId.EXECUTION_METADATA,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_SECONDARY_OVERVIEW),
                summary("ATF execution metadata (secondary)", "secondary metadata"),
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, "",
                                AtfReportHtmlVocabulary.idAttribute(
                                        AtfReportHtmlVocabulary.ElementId.EXECUTION_METADATA_GRID),
                                AtfReportHtmlVocabulary.classAttribute(
                                        AtfReportHtmlVocabulary.CssClass.ATF_SECONDARY_GRID)),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_SECONDARY_BODY)))
                + details(AtfReportHtmlVocabulary.ElementId.TAG_AUDIT,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_SECONDARY_OVERVIEW,
                        AtfReportHtmlVocabulary.CssClass.ATF_TAG_AUDIT_DISCLOSURE),
                summary("ATF tag audit (secondary navigation metadata)", "collapsed by default"),
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                                auditPanels(),
                                AtfReportHtmlVocabulary.classAttribute(
                                        AtfReportHtmlVocabulary.CssClass.ATF_TAG_GRID)),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_TAG_AUDIT_BODY)));
        Matcher marker = CATEGORY_CONTAINER.matcher(html);
        if (!marker.find()) return html;
        return html.substring(0, marker.start()) + metadata + html.substring(marker.start());
    }

    private static String details(AtfReportHtmlVocabulary.ElementId id, String classes,
                                  String summary, String body) {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DETAILS, summary + body,
                AtfReportHtmlVocabulary.idAttribute(id), classes);
    }

    private static String summary(String title, String badge) {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SUMMARY,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, title,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_SECONDARY_OVERVIEW_TITLE))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, badge,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_SECONDARY_OVERVIEW_BADGE)));
    }

    private static String auditPanels() {
        StringBuilder panels = new StringBuilder();
        for (AuditPanel panel : AuditPanel.values()) panels.append(auditPanel(panel));
        return panels.toString();
    }

    private static String auditPanel(AuditPanel panel) {
        StringBuilder headers = new StringBuilder();
        for (String header : panel.headers.split("\\|")) {
            headers.append(AtfReportHtmlVocabulary.element(
                    AtfReportHtmlVocabulary.Tag.TH, header));
        }
        String table = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TABLE,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.THEAD,
                        AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TR,
                                headers.toString()))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TBODY, ""),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TABLE,
                        AtfReportHtmlVocabulary.CssClass.TABLE_SM,
                        AtfReportHtmlVocabulary.CssClass.ATF_TAG_TABLE),
                AtfReportHtmlVocabulary.idAttribute(panel.id));
        String scroll = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, table,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_TAG_SCROLL));
        String body = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, scroll,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.CARD_BODY));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.P, panel.title),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.CARD_HEADER)) + body,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.CARD.value(),
                        AtfReportHtmlVocabulary.CssClass.ATF_TAG_CARD.value(),
                        "atf-tag-card-" + panel.suffix));
    }
}
