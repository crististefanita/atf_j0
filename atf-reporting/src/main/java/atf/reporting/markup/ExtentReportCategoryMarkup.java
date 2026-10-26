package atf.reporting.markup;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

/**
 * Owns the stable category-view shell that the vendor Spark renderer does not
 * emit when a report has no tags. Keeping the shell identical for zero, one
 * and many categories lets the browser audit use one locator contract.
 */
public final class ExtentReportCategoryMarkup {
    private static final String CATEGORY_EMPTY_STATE = "categories";
    /** Vendor insertion boundary: the failure view is the first stable sibling. */
    private static final String VENDOR_FAILURE_VIEW_MARKER = "<div "
            + AtfReportHtmlVocabulary.idAttribute(AtfReportHtmlVocabulary.ViewId.FAILS);

    private ExtentReportCategoryMarkup() {
    }

    public static String ensureView(String html) {
        String categoryId = AtfReportHtmlVocabulary.idAttribute(
                AtfReportHtmlVocabulary.ViewId.CATEGORIES);
        if (html.contains(categoryId)) return html;

        int insertionPoint = html.indexOf(VENDOR_FAILURE_VIEW_MARKER);
        if (insertionPoint < 0) {
            throw new IllegalStateException("Report has no stable insertion point for the category view");
        }
        return html.substring(0, insertionPoint) + emptyView() + html.substring(insertionPoint);
    }

    public static String emptyView() {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                testList() + testContent(),
                AtfReportHtmlVocabulary.idAttribute(
                        AtfReportHtmlVocabulary.ViewId.CATEGORIES),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_WRAPPER,
                        AtfReportHtmlVocabulary.CssClass.ROW,
                        AtfReportHtmlVocabulary.CssClass.VIEW,
                        AtfReportHtmlVocabulary.CssClass.CATEGORY_VIEW,
                        AtfReportHtmlVocabulary.CssClass.ATTRIBUTES_VIEW,
                        AtfReportHtmlVocabulary.CssClass.HIDE));
    }

    private static String testList() {
        String title = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                        AtfReportLabels.CATEGORY,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.FONT_SIZE_14)),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF, ""));
        String filterCount = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, "0",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.BADGE,
                                AtfReportHtmlVocabulary.CssClass.BADGE_PRIMARY)),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF, "#"));
        String tools = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.UL,
                        listItem(title),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TOOLS,
                                AtfReportHtmlVocabulary.CssClass.PULL_LEFT))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.UL,
                        listItem(filterCount),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TOOLS,
                                AtfReportHtmlVocabulary.CssClass.TEXT_RIGHT)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LIST_TOOLS));
        String items = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.UL, "",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TEST_LIST_ITEM)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LIST_WRAPPER,
                        AtfReportHtmlVocabulary.CssClass.SCROLLABLE));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                tools + items,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LIST));
    }

    private static String testContent() {
        String back = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.I, "",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.FA,
                                AtfReportHtmlVocabulary.CssClass.FA_ARROW_LEFT)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.BACK_TO_TEST),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF, "#"));
        String tools = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.UL,
                        listItem(back)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_CONTENT_TOOLS));
        String emptyState = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STRONG,
                        AtfReportLabels.NO_CATEGORIES_AVAILABLE,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_EMPTY_STATE_TITLE))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                        AtfReportLabels.NO_CATEGORY_ANNOTATIONS,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_EMPTY_STATE_COPY)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.DETAIL_BODY,
                        AtfReportHtmlVocabulary.CssClass.ATF_EMPTY_STATE),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.EMPTY_STATE, CATEGORY_EMPTY_STATE),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.ROLE, "status"));
        String detail = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                emptyState,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_CONTENT_DETAIL));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                tools + detail,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_CONTENT,
                        AtfReportHtmlVocabulary.CssClass.SCROLLABLE));
    }

    private static String listItem(String content) {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.LI, content);
    }
}
