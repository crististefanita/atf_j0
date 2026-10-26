package atf.ui.tests.extent.support.uireport.browser;

import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;

/** UI-report style checks that do not assume API-only evidence cards. */
public final class UiReportStyleValidator {
    private final ReportUiLocators locators;

    public UiReportStyleValidator(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Result inspect(Page page, ReportUiContract.Tab tab) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(tab, "tab");
        List<String> violations = new ArrayList<>();
        Locator surface = locators.tabView(page, tab);
        checkVisibleAndSized(surface, tab.name() + " tab", violations);
        if (surface.count() > 0 && surface.isVisible()) {
            String display = surface.evaluate("element => getComputedStyle(element).display").toString();
            String visibility = surface.evaluate("element => getComputedStyle(element).visibility").toString();
            if ("none".equals(display)) violations.add(tab.name() + " tab has display:none");
            if (!"visible".equals(visibility)) violations.add(tab.name()
                    + " tab is not visibility:visible");
        }
        inspectRenderBoundary(page, tab, violations);
        switch (tab) {
            case OVERVIEW -> {
                checkVisibleAndSized(locators.overviewCard(page), "Overview card", violations);
                inspectOverviewTableAccess(page, violations);
            }
            case TESTS -> checkVisibleAndSized(locators.testsList(page),
                    "Tests list", violations);
            case CATEGORIES -> checkVisibleAndSized(locators.categoryList(page),
                    "Categories list", violations);
            case FAILS -> checkVisibleAndSized(locators.failureListSurface(surface),
                    "Fails list", violations);
        }
        return new Result(violations);
    }

    /** Prevent Overview tables from being silently clipped by a narrow pane. */
    @SuppressWarnings("unchecked")
    private static void inspectOverviewTableAccess(Page page, List<String> violations) {
        List<Map<String, Object>> tables = (List<Map<String, Object>>) page.evaluate("""
                () => [...document.querySelectorAll(
                  '.dashboard-view .category-container table,'
                  + '.dashboard-view .sysenv-container table,'
                  + '.category-view table')]
                  .filter(table => {
                    const style = getComputedStyle(table);
                    return style.display !== 'none' && style.visibility !== 'hidden'
                      && table.getClientRects().length > 0;
                  })
                  .map(table => {
                    const container = table.parentElement;
                    const bounds = table.getBoundingClientRect();
                    let scrollContainer = null;
                    for (let ancestor = container; ancestor; ancestor = ancestor.parentElement) {
                      const style = getComputedStyle(ancestor);
                      if (['auto', 'scroll'].includes(style.overflowX)
                          && ancestor.scrollWidth > ancestor.clientWidth + 1) {
                        scrollContainer = ancestor.className || ancestor.tagName.toLowerCase();
                        break;
                      }
                    }
                    return {
                      surface: table.closest('.category-container') ? 'Category'
                        : table.closest('.sysenv-container') ? 'System' : 'Categories',
                      tableWidth: table.getBoundingClientRect().width,
                      viewportWidth: window.innerWidth,
                      left: bounds.left,
                      right: bounds.right,
                      scrollContainer
                    };
                  })
                """);
        for (Map<String, Object> table : tables) {
            double left = number(table, "left");
            double right = number(table, "right");
            double viewportWidth = number(table, "viewportWidth");
            if ((left < -1 || right > viewportWidth + 1) && table.get("scrollContainer") == null) {
                violations.add(table.get("surface") + " table is clipped horizontally: " + table);
            }
        }
    }

    /**
     * Shared browser-level rendering gate.  It deliberately lives beside the
     * tab style checks so every tab receives the same diagnostics instead of
     * relying on one hand-picked screenshot flow.
     */
    @SuppressWarnings("unchecked")
    private void inspectRenderBoundary(Page page, ReportUiContract.Tab tab,
                                       List<String> violations) {
        Map<String, Object> geometry = (Map<String, Object>) page.evaluate("""
                () => {
                  const visible = element => {
                    const style = getComputedStyle(element);
                    return style.display !== 'none' && style.visibility !== 'hidden'
                      && element.getClientRects().length > 0;
                  };
                  const offenders = [];
                  for (const element of document.querySelectorAll('*')) {
                    if (!visible(element)) continue;
                    const style = getComputedStyle(element);
                    const intentionallyScrollable = ['auto', 'scroll', 'clip']
                      .includes(style.overflowX);
                    if (element.scrollWidth > element.clientWidth + 1
                        && !intentionallyScrollable) {
                      const bounds = element.getBoundingClientRect();
                      offenders.push({
                        node: element.tagName.toLowerCase()
                          + (element.id ? '#' + element.id : '')
                          + (element.className && typeof element.className === 'string'
                            ? '.' + element.className.trim().replace(/\s+/g, '.') : ''),
                        clientWidth: element.clientWidth,
                        scrollWidth: element.scrollWidth,
                        left: Math.round(bounds.left),
                        right: Math.round(bounds.right),
                        overflowX: style.overflowX,
                        whiteSpace: style.whiteSpace,
                        parentWidth: element.parentElement?.clientWidth ?? 0,
                        text: (element.innerText || '').trim().slice(0, 72),
                        parent: element.parentElement
                          ? element.parentElement.tagName.toLowerCase()
                            + (element.parentElement.className
                              && typeof element.parentElement.className === 'string'
                              ? '.' + element.parentElement.className.trim().replace(/\s+/g, '.') : '')
                          : ''
                      });
                    }
                  }
                  return {
                    documentScrollWidth: document.documentElement.scrollWidth,
                    documentClientWidth: document.documentElement.clientWidth,
                    bodyScrollWidth: document.body ? document.body.scrollWidth : 0,
                    bodyClientWidth: document.body ? document.body.clientWidth : 0,
                    offenders: offenders.slice(0, 12)
                  };
                }
                """);
        if (number(geometry, "documentScrollWidth")
                > number(geometry, "documentClientWidth") + 1) {
            violations.add("document has horizontal overflow: " + geometry);
        }
        if (number(geometry, "bodyScrollWidth") > number(geometry, "bodyClientWidth") + 1) {
            violations.add("body has horizontal overflow: " + geometry);
        }
        Object offenders = geometry.get("offenders");
        if (offenders instanceof List<?> list && !list.isEmpty()) {
            violations.add("non-scrollable rendered element overflows horizontally: " + list);
        }

        int separators = locators.decorativeSeparators(page).count();
        if (separators != 0) {
            violations.add("decorative separator count must be zero, observed " + separators);
        }

        Locator images = locators.images(page);
        for (int index = 0; index < images.count(); index++) {
            String state = images.nth(index).evaluate(
                    "element => String(element.complete) + '|' + String(element.naturalWidth)")
                    .toString();
            if (!state.startsWith("true|0")) continue;
            violations.add("broken image at index " + index);
        }

        if (tab == ReportUiContract.Tab.TESTS) {
            Locator headers = locators.visibleDisclosureHeaders(page, tab);
            for (int index = 0; index < headers.count(); index++) {
                Locator header = headers.nth(index);
                if (locators.disclosureHeaderNodes(header).count() == 0) continue;
                String state = header.evaluate("element => {"
                        + "const marker=getComputedStyle(element,'::before').content;"
                        + "const title=element.querySelector('.card-title .node');"
                        + "const display=getComputedStyle(element).display;"
                        + "return [marker,display,!!title].join('|');}").toString();
                String[] parts = state.split("\\|", -1);
                boolean glyph = parts.length > 0
                        && (parts[0].contains("＋") || parts[0].contains("−"));
                boolean inline = parts.length > 1 && "flex".equals(parts[1]);
                boolean title = parts.length > 2 && "true".equals(parts[2]);
                if (!glyph || !inline || !title) {
                    violations.add("disclosure marker must be inline before its title at header "
                            + index + ": " + state);
                }
            }
        }
    }

    private static double number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.doubleValue() : 0d;
    }

    private static void checkVisibleAndSized(Locator locator, String name,
                                              List<String> violations) {
        if (locator.count() == 0 || !locator.isVisible()) {
            violations.add(name + " is not visible");
            return;
        }
        var box = locator.boundingBox();
        if (box == null || box.width <= 0 || box.height <= 0) {
            violations.add(name + " has no rendered dimensions");
        }
    }

    public record Result(List<String> violations) {
        public Result {
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean valid() { return violations.isEmpty(); }
    }
}
