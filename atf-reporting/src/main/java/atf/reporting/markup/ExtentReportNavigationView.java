package atf.reporting.markup;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.regex.Pattern;

/**
 * Owns the static navigation and Fails-view markup injected into the vendor
 * document. Runtime behaviour is deliberately kept in the client behaviour
 * layer; this class only establishes the stable DOM contract.
 */
public final class ExtentReportNavigationView {
    private static final String VENDOR_NAVIGATION_ITEM = AtfReportHtmlVocabulary.openTag(
            AtfReportHtmlVocabulary.Tag.LI,
            AtfReportHtmlVocabulary.classAttribute(
                    AtfReportHtmlVocabulary.CssClass.NAV_ITEM,
                    AtfReportHtmlVocabulary.CssClass.DROPDOWN),
            AtfReportHtmlVocabulary.attribute(
                    AtfReportHtmlVocabulary.Attribute.ONCLICK,
                    "toggleView('" + AtfReportHtmlVocabulary.ViewId.OVERVIEW.value() + "')"));
    private static final String VENDOR_TEST_NAVIGATION =
            navigationTarget(AtfReportHtmlVocabulary.ElementId.NAV_TEST);
    private static final String VENDOR_CATEGORY_NAVIGATION =
            navigationTarget(AtfReportHtmlVocabulary.ElementId.NAV_CATEGORY);
    private static final String VENDOR_OVERVIEW_CONTAINER =
            AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                    AtfReportHtmlVocabulary.classAttribute(
                            AtfReportHtmlVocabulary.CssClass.CONTAINER_FLUID,
                            AtfReportHtmlVocabulary.CssClass.P4,
                            AtfReportHtmlVocabulary.CssClass.VIEW,
                            AtfReportHtmlVocabulary.CssClass.DASHBOARD_VIEW));

    private ExtentReportNavigationView() {
    }

    public static String inject(String html) {
        html = replaceOverviewNavigation(html);
        html = html.replace(" " + toggleHandler(
                        AtfReportHtmlVocabulary.ViewId.TESTS), "")
                .replace(" " + toggleHandler(
                        AtfReportHtmlVocabulary.ViewId.CATEGORIES), "");
        html = html.replace(VENDOR_TEST_NAVIGATION,
                        navigationTarget(AtfReportHtmlVocabulary.ElementId.NAV_TEST,
                                AtfReportHtmlVocabulary.ViewId.TESTS))
                .replace(VENDOR_CATEGORY_NAVIGATION,
                        navigationTarget(AtfReportHtmlVocabulary.ElementId.NAV_CATEGORY,
                                AtfReportHtmlVocabulary.ViewId.CATEGORIES));
        return html.replace(VENDOR_OVERVIEW_CONTAINER,
                failureView() + AtfReportHtmlVocabulary.openTag(
                        AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.idAttribute(AtfReportHtmlVocabulary.ViewId.OVERVIEW),
                        AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.TAB,
                                AtfReportHtmlVocabulary.TabKey.OVERVIEW.value()),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.CONTAINER_FLUID,
                                AtfReportHtmlVocabulary.CssClass.P4,
                                AtfReportHtmlVocabulary.CssClass.VIEW,
                                AtfReportHtmlVocabulary.CssClass.DASHBOARD_VIEW)));
    }

    private static String replaceOverviewNavigation(String html) {
        String pattern = "(?s)" + Pattern.quote(VENDOR_NAVIGATION_ITEM) + ".*?</li>";
        return html.replaceFirst(pattern, navigationItem(
                AtfReportHtmlVocabulary.ViewId.FAILS,
                AtfReportLabels.FAILS,
                AtfReportHtmlVocabulary.Icon.BUG)
                + navigationItem(AtfReportHtmlVocabulary.ViewId.OVERVIEW,
                AtfReportHtmlVocabulary.FallbackTab.OVERVIEW.label(),
                AtfReportHtmlVocabulary.Icon.BAR_CHART));
    }

    private static String navigationItem(AtfReportHtmlVocabulary.ViewId view, String label,
                                         AtfReportHtmlVocabulary.Icon icon) {
        AtfReportHtmlVocabulary.ElementId navigationId = switch (view) {
            case FAILS -> AtfReportHtmlVocabulary.ElementId.NAV_FAILS;
            case OVERVIEW -> AtfReportHtmlVocabulary.ElementId.NAV_OVERVIEW;
            default -> throw new IllegalArgumentException("Unsupported navigation view: " + view);
        };
        String link = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                        AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.I, "",
                                AtfReportHtmlVocabulary.classAttribute(
                                        AtfReportHtmlVocabulary.CssClass.FA.value(), icon.value())),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ICO)),
                AtfReportHtmlVocabulary.idAttribute(navigationId),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.DROPDOWN_TOGGLE),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.HREF,
                        "#" + view.value()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.NAVIGATION_VIEW,
                        view.value()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.TITLE, label),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ARIA_LABEL, label));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.LI, link,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.NAV_ITEM,
                        AtfReportHtmlVocabulary.CssClass.DROPDOWN));
    }

    private static String failureView() {
        String list = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.UL,
                        AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.LI,
                                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                                        AtfReportHtmlVocabulary.element(
                                                AtfReportHtmlVocabulary.Tag.SPAN,
                                                AtfReportLabels.FAILS,
                                                AtfReportHtmlVocabulary.classAttribute(
                                                        AtfReportHtmlVocabulary.CssClass.FONT_SIZE_14)),
                                        AtfReportHtmlVocabulary.attribute(
                                                AtfReportHtmlVocabulary.Attribute.HREF,
                                                "#" + AtfReportHtmlVocabulary.ViewId.FAILS.value()))),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TOOLS)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LIST_TOOLS))
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, "",
                        AtfReportHtmlVocabulary.idAttribute(
                                AtfReportHtmlVocabulary.ElementId.FAILURE_LIST),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TEST_LIST_ITEM)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LIST_WRAPPER));
        String detail = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, "",
                        AtfReportHtmlVocabulary.idAttribute(
                                AtfReportHtmlVocabulary.ElementId.FAILURE_DETAIL),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.FAILURE_DETAIL)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_CONTENT_DETAIL));
        String content = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, detail,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_CONTENT,
                        AtfReportHtmlVocabulary.CssClass.OPEN));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, list,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TEST_LIST,
                                AtfReportHtmlVocabulary.CssClass.ATF_FAILS_LIST)) + content,
                AtfReportHtmlVocabulary.idAttribute(AtfReportHtmlVocabulary.ViewId.FAILS),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_WRAPPER,
                        AtfReportHtmlVocabulary.CssClass.ROW,
                        AtfReportHtmlVocabulary.CssClass.VIEW,
                        AtfReportHtmlVocabulary.CssClass.EXCEPTION_VIEW,
                        AtfReportHtmlVocabulary.CssClass.HIDE,
                        AtfReportHtmlVocabulary.CssClass.ATF_FAILS_VIEW),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.TAB,
                        AtfReportHtmlVocabulary.TabKey.FAILS.value()));
    }

    private static String navigationTarget(AtfReportHtmlVocabulary.ElementId id) {
        return AtfReportHtmlVocabulary.idAttribute(id) + " "
                + AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.DROPDOWN_TOGGLE) + " "
                + AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF, "#");
    }

    private static String navigationTarget(AtfReportHtmlVocabulary.ElementId id,
                                           AtfReportHtmlVocabulary.ViewId view) {
        String label = switch (id) {
            case NAV_TEST -> AtfReportLabels.TESTS;
            case NAV_CATEGORY -> AtfReportLabels.CATEGORY;
            default -> throw new IllegalArgumentException("Unsupported navigation link: " + id);
        };
        return AtfReportHtmlVocabulary.idAttribute(id) + " "
                + AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.DROPDOWN_TOGGLE) + " "
                + AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF, "#" + view.value()) + " "
                + AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.NAVIGATION_VIEW, view.value()) + " "
                + AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.TITLE, label) + " "
                + AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.ARIA_LABEL, label);
    }

    private static String toggleHandler(AtfReportHtmlVocabulary.ViewId view) {
        return AtfReportHtmlVocabulary.attribute(
                AtfReportHtmlVocabulary.Attribute.ONCLICK,
                "toggleView('" + view.value() + "')");
    }

}
