package atf.ui.tests.extent.support.inspection.table;

import atf.ui.tests.extent.support.locators.table.TableViewportLocators;
import atf.reporting.support.TestReportPaths;
import com.microsoft.playwright.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Component geometry and failure-path proof against the actual composed renderer styles. */
public final class TableViewportProbe {
    private static final TableViewportLocators LOCATORS = new TableViewportLocators();
    private static final String MEASURE = """
            (table, selectors) => {
              const p = table.parentElement, style = getComputedStyle(p), r = p.getBoundingClientRect();
              const cap = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--atf-table-scroll-height'));
              const messages = [];
              if (!table.getClientRects().length) messages.push('hidden data table');
              if (!p.matches(selectors)) messages.push('missing viewport');
              if (p.tabIndex < 0) messages.push('viewport not keyboard focusable');
              if (p.parentElement.closest(selectors)) messages.push('double viewport');
              if (!Number.isFinite(parseFloat(style.maxHeight))
                  || Math.abs(parseFloat(style.maxHeight) - cap) > 1) messages.push('wrong cap: ' + style.maxHeight);
              if (p.clientHeight > cap + 1) messages.push('viewport exceeds System cap');
              if (style.overflowY !== 'auto') messages.push('vertical overflow is ' + style.overflowY);
              if (style.overflowX !== 'auto') messages.push('horizontal scroll unavailable');
              const borders=parseFloat(style.borderLeftWidth)+parseFloat(style.borderRightWidth);
              const scrollbar=p.scrollHeight>p.clientHeight?p.offsetWidth-p.clientWidth-borders:0;
              const available=r.width-borders-parseFloat(style.paddingLeft)-parseFloat(style.paddingRight)-scrollbar;
              if(Math.abs(table.getBoundingClientRect().width-Math.max(available,parseFloat(getComputedStyle(table).minWidth)||0))>2)
                messages.push('table leaves an artificial horizontal gap');
              const headerCells = Array.from(table.tHead?.rows[0]?.cells || []);
              if (getComputedStyle(table).display !== 'table' || !headerCells.length
                  || table.tHead.getBoundingClientRect().width <= 1
                  || getComputedStyle(table.tHead).clipPath !== 'none') messages.push('column header hidden');
              Array.from(table.tBodies).forEach(body => Array.from(body.rows).forEach(row => {
                const cells = Array.from(row.cells);
                cells.forEach((cell,index) => {
                  const box=cell.getBoundingClientRect(), head=headerCells[index]?.getBoundingClientRect();
                  if(getComputedStyle(cell).display !== 'table-cell' || !head
                      || Math.abs(box.x-head.x)>2 || Math.abs(box.width-head.width)>2
                      || index>0 && Math.abs(box.y-cells[0].getBoundingClientRect().y)>2)
                    messages.push('cell outside its column');
                });
              }));
              p.scrollLeft=p.scrollWidth;
              const finalCell=table.tBodies[0]?.rows[0]?.lastElementChild;
              if(finalCell && finalCell.getBoundingClientRect().right>r.right+2)
                messages.push('last column clipped');
              p.scrollLeft=0;
              const long = p.scrollHeight > p.clientHeight + 1;
              if (!long && p.clientHeight >= cap - 1 && table.getBoundingClientRect().height < cap - 4)
                messages.push('short table reserves full viewport');
              p.scrollTop = p.scrollHeight;
              const last = table.tBodies[0]?.lastElementChild;
              if (last && (!last.getClientRects().length || last.getBoundingClientRect().bottom > r.bottom + 2))
                messages.push('last row clipped');
              const header = table.tHead?.rows[0]?.cells[0];
              if (long && header && (getComputedStyle(header).position !== 'sticky'
                  || Math.abs(header.getBoundingClientRect().top - r.top) > 3)) messages.push('header not sticky');
              const result = {family:table.className, cap, height:p.clientHeight,
                scrollHeight:p.scrollHeight, long, messages};
              p.scrollTop = 0;
              return result;
            }
            """;

    private TableViewportProbe() { }

    public static void verifyGeometryMatrix() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            for (boolean fallback : List.of(false, true)) {
                for (int width : List.of(1440, 1280, 591)) {
                    page.setViewportSize(width, 900);
                    for (int rows : List.of(0, 1, TableViewportReportFixture.LONG_ROWS)) {
                        page.setContent(TableViewportReportFixture.componentHtml(fallback, rows, false));
                        for (int cap : List.of(500, 260)) {
                            setCap(page, cap);
                            verifyAll(LOCATORS.componentTables(page), "component " + fallback + "/" + width + "/" + rows);
                            assertEquals(cap, ((Number) LOCATORS.system(page).evaluate(
                                    "e => parseFloat(getComputedStyle(e).maxHeight)")).intValue());
                        }
                        if (rows == 1) verifyBoundary(page, LOCATORS.componentTables(page).first());
                    }
                    page.setContent(TableViewportReportFixture.componentHtml(fallback, 1, true));
                    verifyAll(LOCATORS.componentTables(page), "single tall cell " + width);
                    assertTrue(measure(LOCATORS.componentTables(page).first()).get("long").equals(true),
                            "A single multi-line row must overflow without a row-count threshold");
                }
            }
        }
    }

    public static void verifyPublishedReports() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            for (boolean fallback : List.of(false, true)) {
                Path report = TableViewportReportFixture.publish(fallback);
                Page page = browser.newPage();
                page.setDefaultTimeout(8_000);
                List<String> errors = new ArrayList<>();
                page.onPageError(errors::add);
                page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
                for (int width : List.of(1440, 1280, 591)) {
                    page.setViewportSize(width, 900);
                    Locator scope;
                    if (fallback) {
                        LOCATORS.fallbackTests(page).click();
                        scope = LOCATORS.fallbackContent(page);
                    } else {
                        LOCATORS.firstOwner(page).click();
                        page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                        scope = LOCATORS.selected(page);
                        expand(scope);
                    }
                    LOCATORS.disclosures(scope).evaluateAll("items => items.forEach(e => e.open = true)");
                    settle(page);
                    Locator tables = LOCATORS.dataTables(scope);
                    assertTrue(tables.count() >= 12, "All phases must expose real rendered tables");
                    List<String> labels = LOCATORS.summaries(scope).allTextContents();
                    if (!fallback) {
                        assertEquals(6, LOCATORS.setup(scope).count(), "SETUP evidence may not silently disappear");
                        assertTrue(LOCATORS.actions(scope).count() >= 6, "direct request actions must be present");
                        assertTrue(labels.stream().anyMatch(s -> s.contains("Setup evidence · 40 checks")), labels.toString());
                        assertTrue(labels.stream().anyMatch(s -> s.contains("Assertions · 40")), labels.toString());
                        assertTrue(labels.stream().anyMatch(s -> s.contains("Playwright action sequence (40 entries)")), labels.toString());
                        assertEquals(0, LOCATORS.screenshots(scope).evaluateAll(
                                "items => items.filter(e => e.closest('" + TableViewportLocators.VIEWPORTS + "')).length"));
                    }
                    assertEquals(6, LOCATORS.assertions(scope).count(), "Every phase has short and long assertions");
                    verifyAll(tables, report + " at " + width);
                    verifyKeyboardAndMouse(page, tables);
                    if (width == 1280) page.screenshot(new Page.ScreenshotOptions().setPath(
                            TestReportPaths.report("table-viewport-"
                                    + (fallback ? "fallback" : "extent") + "-proof.png")));
                    assertEquals(labels, LOCATORS.summaries(scope).allTextContents(), "Measuring scroll must not alter titles/counts");
                    if (!fallback) {
                        LOCATORS.disclosures(scope).evaluateAll("items => items.forEach(e => e.open = false)");
                        LOCATORS.disclosures(scope).evaluateAll("items => items.forEach(e => e.open = true)");
                        settle(page);
                        verifyAll(LOCATORS.dataTables(scope), "reopened " + width);
                    }
                }
                assertTrue(errors.isEmpty(), errors.toString());
                page.close();
            }
        }
    }

    public static void verifyNegativeCases() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            for (String defect : List.of("missing", "cap", "unbounded", "hidden", "double", "header", "last-row",
                    "column-header", "stacked", "horizontal")) {
                page.setContent(TableViewportReportFixture.componentHtml(false, defect.equals("unbounded") ? 1 : 40, false));
                Locator table = LOCATORS.componentTables(page).first();
                table.evaluate("""
                        (t, defect) => {
                          const p=t.parentElement;
                          if(defect==='missing') p.replaceWith(t);
                          if(defect==='cap') p.style.setProperty('max-height','100px','important');
                          if(defect==='unbounded') p.style.setProperty('max-height','none','important');
                          if(defect==='hidden') p.style.setProperty('overflow-y','hidden','important');
                          if(defect==='double') {const outer=p.cloneNode(false);p.replaceWith(outer);outer.appendChild(p);}
                          if(defect==='header') t.querySelector('th').style.setProperty('position','static','important');
                          if(defect==='column-header') t.tHead.style.setProperty('clip-path','inset(50%)','important');
                          if(defect==='stacked') Array.from(t.tBodies[0].rows).forEach(row=>Array.from(row.cells)
                            .forEach(cell=>cell.style.setProperty('display','block','important')));
                          if(defect==='horizontal') p.style.setProperty('overflow-x','hidden','important');
                          if(defect==='last-row') {
                            t.style.setProperty('display','block','important');
                            t.style.setProperty('height','100px','important');
                            t.style.setProperty('overflow','hidden','important');
                          }
                        }
                        """, defect);
                List<?> failures = messages(measure(table));
                assertFalse(failures.isEmpty(), "Probe must reject deliberate defect: " + defect);
                if (defect.equals("last-row")) assertTrue(failures.contains("last row clipped"), failures.toString());
            }
        }
    }

    private static void expand(Locator scope) {
        // Click the actual semantic headers, ancestors before descendants; no synthetic row creation.
        Locator cards = LOCATORS.nativeCards(scope);
        for (int i = 0; i < cards.count(); i++) {
            Locator card = cards.nth(i);
            Locator header = LOCATORS.headers(card);
            if (header.count() > 0 && header.first().isVisible()
                    && !"true".equals(header.first().getAttribute("aria-expanded"))) header.first().click();
        }
    }

    private static void verifyAll(Locator tables, String context) {
        assertTrue(tables.count() > 0, "Missing data-table inventory: " + context);
        Object raw = tables.evaluateAll("(tables, selectors) => tables.map(table => ("
                + MEASURE + ")(table, selectors))", TableViewportLocators.VIEWPORTS);
        for (Object item : (List<?>) raw) {
            Map<?, ?> result = (Map<?, ?>) item;
            assertTrue(messages(result).isEmpty(), context + ": " + result);
        }
    }

    private static Map<?, ?> measure(Locator table) {
        return (Map<?, ?>) table.evaluate(MEASURE, TableViewportLocators.VIEWPORTS);
    }

    private static void verifyKeyboardAndMouse(Page page, Locator tables) {
        for (int i = 0; i < tables.count(); i++) {
            Locator table = tables.nth(i);
            if (!Boolean.TRUE.equals(measure(table).get("long"))) continue;
            Locator viewport = LOCATORS.viewport(table);
            page.bringToFront();
            viewport.scrollIntoViewIfNeeded();
            viewport.focus();
            assertEquals(Boolean.TRUE, viewport.evaluate("e => e === document.activeElement"),
                    "A data viewport must accept keyboard focus");
            viewport.press("End");
            try {
                page.waitForFunction("e => e.scrollTop >= e.scrollHeight - e.clientHeight - 1", viewport.elementHandle());
            } catch (com.microsoft.playwright.TimeoutError error) {
                throw new AssertionError("End did not scroll the data viewport: " + viewport.evaluate("""
                        e => ({family:e.className,focus:document.activeElement.outerHTML.slice(0,150),
                          connected:e.isConnected,top:e.scrollTop,height:e.clientHeight,total:e.scrollHeight,
                          overflow:getComputedStyle(e).overflowY,documentFocus:document.hasFocus(),
                          rectangle:e.getBoundingClientRect().toJSON()})
                        """), error);
            }
            viewport.press("Home");
            page.waitForFunction("e => e.scrollTop === 0", viewport.elementHandle());
            viewport.hover();
            Object parentScroll = outerScroll(viewport);
            page.mouse().wheel(0, 200);
            page.waitForFunction("e => e.scrollTop > 0", viewport.elementHandle());
            assertEquals(parentScroll, outerScroll(viewport), "A local wheel gesture must not move the host pane");
            page.mouse().wheel(0, 10_000);
            page.waitForFunction("e => e.scrollTop >= e.scrollHeight - e.clientHeight - 1", viewport.elementHandle());
            assertEquals(parentScroll, outerScroll(viewport), "The final row must be reachable without scroll chaining");
            assertTrue(messages(measure(table)).isEmpty(), "Keyboard/mouse scroll must preserve geometry");
            if (Boolean.TRUE.equals(viewport.evaluate("e => e.scrollWidth > e.clientWidth+2"))) {
                viewport.focus();
                viewport.press("ArrowRight");
                page.waitForFunction("e => e.scrollLeft > 0", viewport.elementHandle());
                viewport.press("ArrowLeft");
                page.waitForFunction("e => e.scrollLeft === 0", viewport.elementHandle());
                assertEquals(parentScroll, outerScroll(viewport), "Horizontal navigation remains local");
            }
            return;
        }
        fail("The fixture must expose a long table for keyboard/mouse proof");
    }

    private static List<?> messages(Map<?, ?> measured) { return (List<?>) measured.get("messages"); }

    private static Object outerScroll(Locator viewport) {
        return viewport.evaluate("e => {const values=[];for(let p=e.parentElement;p;p=p.parentElement)"
                + "values.push(p.scrollTop);return values;}");
    }

    private static void verifyBoundary(Page page, Locator table) {
        int natural = ((Number) measure(table).get("scrollHeight")).intValue();
        for (int delta : List.of(2, 0, -2)) {
            setCap(page, natural + delta);
            Map<?, ?> measured = measure(table);
            assertTrue(messages(measured).isEmpty(), "At the viewport boundary: " + measured);
            assertEquals(delta < 0, measured.get("long"), "Overflow follows rendered height, not cardinality");
        }
    }

    private static void setCap(Page page, int cap) {
        page.evaluate("cap => document.documentElement.style.setProperty('--atf-table-scroll-height',cap+'px')", cap);
    }

    private static void settle(Page page) {
        page.evaluate("() => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))");
    }
}
