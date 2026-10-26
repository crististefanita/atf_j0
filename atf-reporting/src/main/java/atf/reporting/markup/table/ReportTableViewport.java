package atf.reporting.markup.table;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Renderer-owned table presentation; aliases preserve existing selectors and evidence titles. */
public enum ReportTableViewport {
    DATA("atf-table-viewport"),
    ACTIONS("atf-ui-action-table-wrap"),
    ASSERTIONS("atf-validation-table-wrap"),
    TRACE("atf-trace-table-wrap"),
    API("atf-api-table-wrap");

    private final String cssClass;

    ReportTableViewport(String cssClass) {
        this.cssClass = cssClass;
    }

    public String cssClass() {
        return cssClass;
    }

    public String open() {
        return AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.classAttribute(cssClass), "tabindex=\"0\"");
    }

    public String wrap(String table) {
        return open() + table + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV);
    }

    /** One component vocabulary for both renderers; not a list of business table names. */
    public static String selectors() {
        return Arrays.stream(values()).map(value -> "." + value.cssClass)
                .collect(Collectors.joining(","));
    }

    /** Vendor containers already own their table; adapt them without nesting a second viewport. */
    public static String scrollSurfaces() {
        return selectors() + ",.atf-tag-scroll,.atf-secondary-body.network-aggregate-scroll,"
                + ".dashboard-view .category-container > .card > .card-body,"
                + ".dashboard-view .sysenv-container > .card > .card-body";
    }
}
