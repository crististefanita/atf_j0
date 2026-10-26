package atf.ui.tests.extent.support.locators;

import atf.reporting.theme.tokens.AtfReportCssSelectors;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Selectors and replay actions for the report category view. */
final class ReportCategoryLocators {
    private static final String CATEGORY_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.CATEGORY_VIEW);
    private static final String TEST_LIST = AtfReportCssSelectors.Component.TEST_LIST.attributeSelector();
    private static final String TEST_ITEM = AtfReportCssSelectors.Component.TEST_ITEM.attributeSelector();
    private static final String CATEGORY_ITEMS = TEST_LIST + " " + TEST_ITEM;
    private static final String TEST_CONTENT_DETAIL =
            AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector();
    private static final String TEST_DETAIL = AtfReportCssSelectors.Component.TEST_DETAIL.attributeSelector();
    private static final String TEST_LIST_TOOLS = AtfReportCssSelectors.Component.TEST_LIST_TOOLS.attributeSelector();

    Locator items(Page page) { return page.locator(CATEGORY_VIEW).locator(CATEGORY_ITEMS); }
    Locator item(Locator items, int index) { return items.nth(index); }
    Locator itemNames(Locator items) { return items.locator(TEST_DETAIL + " " + classToken("name")); }
    Locator itemName(Locator item) { return item.locator(TEST_DETAIL + " " + classToken("name")); }
    Locator itemDuration(Locator item) { return item.locator(classToken("duration")); }
    Locator statusMenu(Locator category) { return category.locator(ReportLocatorSupport.idSelector("category-status-toggle")); }
    Locator tagMenu(Locator category) { return category.locator(ReportLocatorSupport.idSelector("category-tag-toggle")); }
    Locator statusFilterTrigger(Locator category) {
        return category.locator(TEST_LIST_TOOLS + " a[aria-label='Filter categories by status']");
    }
    Locator tagFilterTrigger(Locator category) {
        return category.locator(TEST_LIST_TOOLS + " a[aria-label='Filter categories by tag']");
    }
    Locator aggregateStatuses(Locator category) {
        return category.locator(CATEGORY_ITEMS + " "
                + AtfReportCssSelectors.Component.TEST_CONTENTS.attributeSelector() + " "
                + classToken("info") + " [status]");
    }
    Locator statusOption(Locator menu, String status) {
        return menu.locator(classToken("dropdown-item") + "[data-filter-value='" + cssAttribute(status) + "']");
    }
    Locator statusOptions(Locator category) {
        return statusMenu(category).locator(classToken("dropdown-item") + "[data-filter-value]");
    }
    AtfLocator statusFilterTriggerAction(Page page) {
        String selector = CATEGORY_VIEW + " " + TEST_LIST_TOOLS
                + " a[aria-label='Filter categories by status']";
        return new AtfLocator(page.locator(selector), selector);
    }
    AtfLocator statusOptionAction(Page page, String status) {
        String safeStatus = cssAttribute(status);
        String selector = CATEGORY_VIEW + " " + ReportLocatorSupport.idSelector("category-status-toggle")
                + " " + classToken("dropdown-item") + "[data-filter-value='" + safeStatus + "']";
        return new AtfLocator(page.locator(selector), selector);
    }
    Locator tagOptions(Locator category) {
        return tagMenu(category)
                .locator(classToken("dropdown-item")
                        + "[data-filter-kind='tag']:not([data-filter-value='clear'])");
    }
    Locator clearFilter(Locator categoryOrMenu) {
        Locator menu = categoryOrMenu.locator(ReportLocatorSupport.idSelector("category-tag-toggle"));
        return (menu.count() > 0 ? menu : categoryOrMenu)
                .locator(classToken("dropdown-item") + "[data-filter-value='clear']");
    }
    Locator visibleItems(Locator category) {
        return category.locator(CATEGORY_ITEMS + "[data-atf-filter-hidden='false']");
    }
    Locator visibleDetail(Page page) {
        return page.locator(CATEGORY_VIEW).locator(
                AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector() + ":visible");
    }
    Locator title(Locator detail) {
        return detail.locator("[data-atf-category-title='true'], h4").first();
    }
    Locator linkedTests(Locator detail) { return detail.locator("a[class~='linked']:visible"); }
    Locator statusRows(Locator detail) { return detail.locator("tr[class~='tag-test-status']"); }
    AtfLocator itemAction(Page page, int zeroBasedIndex) {
        String selector = itemSelector(zeroBasedIndex);
        return new AtfLocator(page.locator(selector), selector);
    }
    AtfLocator visibleDetailAnchor(Page page) {
        String selector = CATEGORY_VIEW + " "
                + AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector() + ":visible";
        return new AtfLocator(page.locator(selector), selector);
    }
    AtfLocator firstItemAction(Page page) {
        String selector = CATEGORY_VIEW + " " + CATEGORY_ITEMS + ":visible:first-of-type";
        return new AtfLocator(page.locator(CATEGORY_VIEW).locator(CATEGORY_ITEMS + ":visible").first(), selector);
    }
    Locator surface(Page page) { return page.locator(CATEGORY_VIEW).locator(TEST_LIST); }
    Locator content(Page page) {
        return page.locator(CATEGORY_VIEW)
                .locator(AtfReportCssSelectors.Component.TEST_CONTENT.attributeSelector());
    }
    Locator content(Locator category) {
        return category.locator(AtfReportCssSelectors.Component.TEST_CONTENT.attributeSelector());
    }
    AtfLocator firstLinkAction(Page page) {
        return new AtfLocator(linkedTests(page.locator(CATEGORY_VIEW).locator(TEST_CONTENT_DETAIL)).first(),
                CATEGORY_VIEW + " a[class~='linked']:first-of-type");
    }
    String itemSelector(int zeroBasedIndex) {
        if (zeroBasedIndex < 0) throw new IllegalArgumentException("zeroBasedIndex must be >= 0");
        return CATEGORY_VIEW + " " + CATEGORY_ITEMS + ":nth-of-type(" + (zeroBasedIndex + 1) + ")";
    }

    private static String cssAttribute(String value) {
        return ReportLocatorSupport.cssAttribute(value);
    }

    private static String classToken(String value) {
        return ReportLocatorSupport.classSelector(value);
    }
}
