package atf.ui.tests.extent.support.locators;

import atf.reporting.theme.tokens.AtfReportCssSelectors;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Map;

/** Selectors and replay actions for the report failure view. */
final class ReportFailureLocators {
    private static final String FAILS_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.FAILS_VIEW);
    private static final String TEST_LIST = AtfReportCssSelectors.Component.TEST_LIST.attributeSelector();
    private static final String TEST_CONTENT = AtfReportCssSelectors.Component.TEST_CONTENT.attributeSelector();
    private static final String FAILURE_ITEMS = "[id='atf-failure-list'] > "
            + ReportLocatorSupport.classSelector("test-item");

    Locator names(Locator fails) {
        return fails.locator("[id='atf-failure-list'] " + ReportLocatorSupport.classSelector("name"));
    }
    Locator items(Locator fails) { return fails.locator(FAILURE_ITEMS); }
    Locator itemByName(Locator fails, String name) {
        return items(fails).filter(new Locator.FilterOptions().setHasText(name));
    }
    Locator listSurface(Locator fails) { return fails.locator(TEST_LIST); }
    Locator detailSurface(Locator fails) { return fails.locator(TEST_CONTENT); }
    Locator openLinks(Locator fails) {
        return fails.locator("a" + ReportLocatorSupport.classSelector("atf-failure-open"));
    }
    Locator detail(Locator fails) { return fails.locator("[id='atf-failure-detail']"); }
    Locator detailLabels(Locator fails) {
        return detail(fails).locator(ReportLocatorSupport.classSelector("atf-failure-label"));
    }
    Locator detailTitle(Locator fails) {
        return detail(fails).locator(ReportLocatorSupport.classSelector("atf-failure-index-title"));
    }
    Locator detailOpenLink(Locator fails) {
        return detail(fails).locator(ReportLocatorSupport.classSelector("atf-failure-open"));
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> layout(Locator fails) {
        return (Map<String, Object>) fails.evaluate("""
                element => {
                  const list = element.querySelector(%s);
                  const detail = element.querySelector(%s);
                  const box = node => {
                    if (!node) return {x: 0, y: 0, width: 0, height: 0};
                    const rect = node.getBoundingClientRect();
                    return {x: rect.x, y: rect.y, width: rect.width, height: rect.height};
                  };
                  return {
                    flexDirection: getComputedStyle(element).flexDirection,
                    overflowX: getComputedStyle(element).overflowX,
                    scrollWidth: element.scrollWidth,
                    clientWidth: element.clientWidth,
                    documentScrollWidth: document.documentElement.scrollWidth,
                    viewportWidth: window.innerWidth,
                    list: box(list),
                    detail: box(detail)
                  };
                }
                """.formatted(jsString(TEST_LIST), jsString(TEST_CONTENT)));
    }
    Locator causeValues(Locator fails) {
        return fails.locator(ReportLocatorSupport.classSelector("atf-failure-index-cause") + " "
                + ReportLocatorSupport.classSelector("atf-failure-value"));
    }
    Locator detailPrimaryCauses(Locator fails) {
        return detail(fails).getByText("PRIMARY:", new Locator.GetByTextOptions().setExact(false));
    }
    Locator detailSecondaryCauses(Locator fails) {
        return detail(fails).getByText("SECONDARY:", new Locator.GetByTextOptions().setExact(false));
    }
    Locator emptyState(Locator fails) {
        return fails.locator("[id='atf-failure-list'] "
                + ReportLocatorSupport.classSelector("atf-fails-empty"));
    }
    AtfLocator itemAction(Page page, int zeroBasedIndex) {
        int index = requireIndex(zeroBasedIndex);
        return new AtfLocator(items(page.locator(FAILS_VIEW)).nth(index),
                FAILS_VIEW + " " + FAILURE_ITEMS + ":nth-of-type(" + (index + 1) + ")");
    }
    AtfLocator itemActionByName(Page page, String name) {
        return new AtfLocator(itemByName(page.locator(FAILS_VIEW), name).first(),
                FAILS_VIEW + " " + FAILURE_ITEMS + ":has-text(\"" + textSelector(name) + "\")");
    }

    private static String textSelector(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    AtfLocator firstOpenAction(Page page) {
        return new AtfLocator(openLinks(page.locator(FAILS_VIEW)).first(),
                FAILS_VIEW + " a" + ReportLocatorSupport.classSelector("atf-failure-open")
                        + ":first-of-type");
    }
    AtfLocator detailOpenAction(Page page) {
        return new AtfLocator(detailOpenLink(page.locator(FAILS_VIEW)),
                FAILS_VIEW + " [id='atf-failure-detail'] a"
                        + ReportLocatorSupport.classSelector("atf-failure-open"));
    }
    Locator primaryDetails(Locator fails) {
        return fails.getByText("PRIMARY:", new Locator.GetByTextOptions().setExact(false));
    }

    private static int requireIndex(int zeroBasedIndex) {
        if (zeroBasedIndex < 0) throw new IllegalArgumentException("zeroBasedIndex must be >= 0");
        return zeroBasedIndex;
    }
}
