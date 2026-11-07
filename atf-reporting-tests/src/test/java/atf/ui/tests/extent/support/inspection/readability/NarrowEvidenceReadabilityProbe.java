package atf.ui.tests.extent.support.inspection.readability;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.markup.table.ReportTableViewport;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import atf.ui.tests.extent.support.locators.readability.ReportReadabilityLocators;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests actual evidence widths, labels, complete values, local scroll and accessible final rows. */
public final class NarrowEvidenceReadabilityProbe {
    private static final int MINIMUM_READABLE_PANEL_WIDTH = 120;
    private static final ReportDisclosureLocators LOCATORS = new ReportDisclosureLocators();

    private static final ReportReadabilityLocators READABILITY = new ReportReadabilityLocators();

    private NarrowEvidenceReadabilityProbe() { }

    public static void verify(Path report, boolean fallback) {
        verify(report, fallback, false);
    }

    public static void verifyFailure(Path report) {
        verify(report, false, true);
    }

    private static void verify(Path report, boolean fallback, boolean failure) {
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)))) {
            try {
                var browser = browserLease.value();
                var page = BrowserResources.newPage(browser);
                var errors = new ArrayList<String>();
                page.onPageError(errors::add);
                page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
                if (fallback) LOCATORS.fallbackTestsTab(page).click();
                else {
                    LOCATORS.firstOwner(page).click();
                    READABILITY.expandAll(page).click();
                }
                READABILITY.details(page).evaluateAll("items => items.forEach(d => d.open = true)");
                if (failure) assertTrue(READABILITY.failureEvidence(page).count() > 0,
                        "the negative fixture must exercise the real Failure evidence validation node");
                var failures = new ArrayList<String>();
                int minimumTables = failure ? 4 : 12;
                for (int viewport : List.of(1803, 1280, 591, 392)) {
                    page.setViewportSize(viewport, 900);
                    measure(page, "viewport=" + viewport, failures, minimumTables);
                }
                page.setViewportSize(1280, 900);
                for (int panel : List.of(591, 392, 719, 720, 721)) {
                    READABILITY.panel(page, fallback).evaluateAll(
                            "(items,width) => items.forEach(e => e.style.width=width+'px')", panel);
                    measure(page, "wide viewport; panel=" + panel, failures, minimumTables);
                }
                if (failure) {
                    var common = new ReportUiLocators();
                    page.setViewportSize(392, 900);
                    READABILITY.failsTab(page).click();
                    common.failureDetailOpenLink(common.failsView(page)).waitFor();
                    READABILITY.comparisonFailure(page, 1).click();
                    assertTrue(common.failsView(page).innerText().contains("Independent comparison 40"),
                            "Fails preserves the original failed comparison before its backlink");
                    common.failureDetailOpenLink(common.failsView(page)).click();
                    assertTrue(READABILITY.testsView(page).isVisible(), "failure backlink selects Tests");
                    assertTrue(common.visibleTestDetail(page).innerText().contains("NarrowEvidenceFailure"));
                    READABILITY.expandAll(page).click();
                    var scenario = LOCATORS.scenarios(page).first();
                    var request = LOCATORS.requests(LOCATORS.firstPhase(scenario)).first();
                    LOCATORS.header(request).click();
                    LOCATORS.header(request).click();
                    READABILITY.details(page).evaluateAll("items => items.forEach(d => d.open = true)");
                    var summary = READABILITY.firstVisibleSummary(request);
                    summary.press("Enter");
                    assertTrue(!(Boolean) summary.evaluate("s => s.parentElement.open"));
                    summary.press("Space");
                    assertTrue((Boolean) summary.evaluate("s => s.parentElement.open"));
                    measure(page, "Tests after parent and keyboard reopening", failures, minimumTables);
                }
                assertTrue(errors.isEmpty(), errors::toString);
                assertTrue(failures.isEmpty(), () -> report.getFileName() + ": " + failures);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void measure(Page page, String context, List<String> failures, int minimumTables) {
        page.evaluate("() => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))");
        var rows = (List<Map<String, Object>>) page.evaluate("""
                args => Array.from(document.querySelectorAll(args.surfaces)).flatMap(p => {
                  const t=Array.from(p.children).find(e=>e.tagName==='TABLE');
                  if(!t || !t.getBoundingClientRect().height) return [];
                  const messages=[];
                  const style=getComputedStyle(p), head=t.tHead?.getBoundingClientRect();
                  const borders=parseFloat(style.borderLeftWidth)+parseFloat(style.borderRightWidth);
                  const scrollbar=p.scrollHeight>p.clientHeight?p.offsetWidth-p.clientWidth-borders:0;
                  const available=p.getBoundingClientRect().width-borders-parseFloat(style.paddingLeft)
                    -parseFloat(style.paddingRight)-scrollbar;
                  if(Math.abs(t.getBoundingClientRect().width-Math.max(available,parseFloat(getComputedStyle(t).minWidth)||0))>2)
                    messages.push('table leaves an artificial horizontal gap');
                  if(p.clientWidth<args.minimum) messages.push('evidence viewport too narrow: '+p.clientWidth);
                  if(style.overflowX!=='auto') messages.push('horizontal scroll unavailable');
                  if(document.documentElement.scrollWidth>document.documentElement.clientWidth+2)
                    messages.push('document horizontal overflow');
                  if(getComputedStyle(t).display!=='table' || !head || head.width<=1
                      || getComputedStyle(t.tHead).clipPath!=='none') messages.push('column header hidden');
                  const cells=Array.from(t.tBodies).flatMap(b=>Array.from(b.rows)).flatMap(r=>Array.from(r.cells));
                  cells.forEach(c=>{
                    const box=c.getBoundingClientRect(), column=t.tHead.rows[0].cells[c.cellIndex]?.getBoundingClientRect();
                    if(getComputedStyle(c).display!=='table-cell' || !column || Math.abs(box.x-column.x)>2
                        || Math.abs(box.width-column.width)>2) messages.push('cell outside its column');
                    if(parseFloat(getComputedStyle(c).fontSize)<12) messages.push('cell font reduced');
                    if(c.scrollHeight>c.clientHeight+2 || c.scrollWidth>c.clientWidth+2)
                      messages.push('cell text clipped');
                  });
                  p.scrollLeft=p.scrollWidth;
                  const lastColumn=t.tBodies[0]?.rows[0]?.lastElementChild;
                  if(lastColumn && lastColumn.getBoundingClientRect().right>p.getBoundingClientRect().right+2)
                    messages.push('final column clipped');
                  p.scrollLeft=0;
                  const cap=parseFloat(getComputedStyle(p).maxHeight);
                  if(p.clientHeight>cap+2) messages.push('scroll cap exceeded');
                  p.scrollTop=p.scrollHeight;
                  const last=t.tBodies[t.tBodies.length-1]?.lastElementChild;
                  if(last && last.getBoundingClientRect().bottom>p.getBoundingClientRect().bottom+2)
                    messages.push('final row clipped');
                  p.scrollTop=0;
                  return [{family:t.className,width:p.clientWidth,messages:[...new Set(messages)]}];
                })
                """, Map.of("surfaces", ReportTableViewport.tableSurfaces(),
                "minimum", MINIMUM_READABLE_PANEL_WIDTH));
        assertTrue(rows.size() >= minimumTables, "all real evidence families must be measured: " + context);
        rows.stream().filter(row -> !((List<?>) row.get("messages")).isEmpty())
                .forEach(row -> failures.add(context + " " + row));
    }
}
