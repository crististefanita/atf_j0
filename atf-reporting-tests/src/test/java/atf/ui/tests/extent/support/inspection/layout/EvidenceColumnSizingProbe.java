package atf.ui.tests.extent.support.inspection.layout;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.theme.AtfReportTheme;
import atf.ui.tests.extent.support.locators.table.TableViewportLocators;
import com.microsoft.playwright.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Independent browser geometry; does not invoke the allocator or derive its expected widths. */
public final class EvidenceColumnSizingProbe {
    private static final TableViewportLocators LOCATORS = new TableViewportLocators();
    private static final String LONG = "Observed business state contains an independently checked meaningful value. ".repeat(8);

    private EvidenceColumnSizingProbe() { }

    public static void verifyAllocation(boolean fallback) {
        try (var fixture = new Fixture(fallback)) {
            for (int width : List.of(920, 719, 720, 721, 548, 392)) {
                fixture.page.setViewportSize(width + 16, 900);
                settle(fixture.page);
                for (var table : fixture.tables()) verifyColumns(table);
                assertDominant(fixture.tables().get(0), 2, 1, width >= 548 ? 1.5 : 1.0);
                assertDominant(fixture.tables().get(1), 1, 3, width >= 548 ? 1.5 : 1.0);
                assertDominant(fixture.tables().get(2), 0, 1);
                var actions = widths(fixture.tables().get(1));
                assertTrue(actions.get(4) < 100 && actions.get(5) < 100, "short time/result remain compact: " + actions);
                if (width >= 548) for (var table : fixture.tables()) assertEquals(Boolean.TRUE,
                        LOCATORS.viewport(table).evaluate("e => e.scrollWidth<=e.clientWidth+2"),
                        "ordinary desktop evidence fits without a forced table width");
                assertEquals(Boolean.TRUE, fixture.page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
                var viewport = LOCATORS.viewport(fixture.tables().get(1));
                if (width == 392 && Boolean.TRUE.equals(viewport.evaluate("e => e.scrollWidth>e.clientWidth+2"))) {
                    fixture.page.bringToFront();
                    viewport.focus();
                    viewport.press("ArrowRight");
                    fixture.page.waitForFunction("e => e.scrollLeft>0", viewport.elementHandle());
                    viewport.press("ArrowLeft");
                    fixture.page.waitForFunction("e => e.scrollLeft===0", viewport.elementHandle());
                }
            }
            fixture.page.setViewportSize(936, 900);
            settle(fixture.page);
            verifyColumns(fixture.tables().get(3));
            verifyColumns(fixture.tables().get(4));
        }
    }

    public static void verifyInvalidation(boolean fallback) {
        try (var fixture = new Fixture(fallback)) {
            Locator table = fixture.tables().get(0), unrelated = fixture.tables().get(1);
            String other = unrelated.getAttribute("data-atf-content-column-max-px");
            table.evaluate("(t,text) => {t.tBodies[0].rows[0].cells[1].textContent=text;t.tBodies[0].rows[0].cells[2].textContent='OK';}", LONG);
            fixture.page.waitForFunction("t => t.rows[0].cells[1].getBoundingClientRect().width>t.rows[0].cells[2].getBoundingClientRect().width*1.5",
                    table.elementHandle());
            assertDominant(table, 1, 2);
            assertEquals(other, unrelated.getAttribute("data-atf-content-column-max-px"), "unrelated text metrics remain stable");
            var viewport = LOCATORS.viewport(table);
            viewport.evaluate("e => e.style.display='none'");
            settle(fixture.page);
            viewport.evaluate("e => e.style.display=''");
            fixture.page.setViewportSize(564, 900);
            settle(fixture.page);
            verifyColumns(table);
            assertDominant(table, 1, 2);
            String future = AtfReportMarkup.tableHtml("helm-evidence-table atf-ui-failure-table future-column-family-late", new String[][] {
                    {"CHECK", "EXPECTED", "ACTUAL", "RESULT"}, {"Future", "OK", LONG, "PASS"}});
            future = future.substring(future.indexOf('>') + 1, future.lastIndexOf("</div>"));
            table.evaluate("(t,html) => {const host=document.createElement('section');host.setAttribute('data-family','late');"
                    + "host.innerHTML=html;document.body.appendChild(host);}", future);
            fixture.page.waitForFunction("() => document.querySelector('table.future-column-family-late')?.hasAttribute('data-atf-content-column-widths')");
            Locator added = fixture.tables().getLast();
            verifyColumns(added);
            assertDominant(added, 2, 1);
        }
    }

    public static void verifyNegative(boolean fallback) {
        try (var fixture = new Fixture(fallback)) {
            Locator table = fixture.tables().get(0);
            table.evaluate("t => Array.from(t.rows).forEach(r=>Array.from(r.cells).forEach(c=>c.style.setProperty('width','25%','important')))");
            assertThrows(AssertionError.class, () -> assertDominant(table, 2, 1), "markers alone cannot certify measured widths");
            Locator shortTable = fixture.tables().get(4);
            shortTable.evaluate("t => t.style.setProperty('width','50%','important')");
            assertThrows(AssertionError.class, () -> verifyColumns(shortTable), "artificial right-hand gaps are rejected");
            shortTable.evaluate("t => {t.style.removeProperty('width');t.parentElement.style.setProperty('scrollbar-gutter','stable','important');}");
            assertThrows(AssertionError.class, () -> verifyColumns(shortTable), "idle scrollbar reservations are artificial gaps too");
            table.evaluate("t => Array.from(t.tBodies[0].rows[0].cells).forEach(c=>c.style.setProperty('display','block','important'))");
            assertThrows(AssertionError.class, () -> verifyColumns(table));
        }
    }

    public static void verifyCompetingValues(boolean fallback) {
        try (var fixture = new Fixture(fallback)) {
            for (Locator table : List.of(fixture.tables().getFirst(), fixture.tables().get(5))) {
                table.evaluate("t => {t.tBodies[0].rows[0].cells[1].textContent='i'.repeat(160);"
                        + "t.tBodies[0].rows[0].cells[2].textContent='W'.repeat(160);}");
                fixture.page.waitForFunction("t => {const m=t.getAttribute('data-atf-content-column-max-px').split('|').map(parseFloat);"
                        + "return m[1]>200&&m[2]>m[1]*2;}", table.elementHandle());
                assertDominant(table, 2, 1);
                assertTrue(((Number) table.evaluate("t => parseFloat(t.getAttribute('data-atf-content-column-max-px').split('|')[2])"))
                        .doubleValue() > 560, "pixel maxima must retain unequal long messages rather than saturating");
                table.evaluate("t => {t.tBodies[0].rows[0].cells[1].textContent='W'.repeat(200);"
                        + "t.tBodies[0].rows[0].cells[2].textContent='i'.repeat(160);}");
                fixture.page.waitForFunction("t => {const m=t.getAttribute('data-atf-content-column-max-px').split('|').map(parseFloat);"
                        + "return m[1]>m[2]*2;}", table.elementHandle());
                assertDominant(table, 1, 2);
                verifyColumns(table);
            }
        }
    }

    public static void verifyExistingOwners(boolean fallback) {
        try (var fixture = new Fixture(fallback)) {
            for (Locator table : fixture.tables().subList(6, 9)) {
                verifyColumns(table);
                assertDominant(table, 0, 1);
                assertEquals(Boolean.TRUE, table.evaluate("t => !t.parentElement.classList.contains('atf-table-viewport')"),
                        "dashboard tables reuse their existing owner, without a nested viewport");
                assertTrue(widths(table).subList(1, 4).stream().allMatch(width -> width < 100),
                        "short numeric totals do not absorb the free content width");
                assertEquals(Boolean.TRUE, table.evaluate("t => Array.from(t.querySelectorAll('col')).every((c,i)=>"
                        + "Math.abs(c.getBoundingClientRect().width-t.rows[0].cells[i].getBoundingClientRect().width)<2)"),
                        "preexisting colgroups agree with the measured header/body allocation");
            }
        }
    }

    private static void assertDominant(Locator table, int longer, int shorter) {
        assertDominant(table, longer, shorter, 1.5);
    }

    private static void assertDominant(Locator table, int longer, int shorter, double ratio) {
        List<Double> values = widths(table);
        assertTrue(values.get(longer) > values.get(shorter) * ratio, "long content must receive more space: " + values);
    }

    @SuppressWarnings("unchecked")
    private static List<Double> widths(Locator table) {
        return ((List<Number>) table.evaluate("t => Array.from(t.rows[0].cells,c=>c.getBoundingClientRect().width)"))
                .stream().map(Number::doubleValue).toList();
    }

    private static void verifyColumns(Locator table) {
        assertEquals("dynamic-longest-value", table.getAttribute("data-atf-content-layout"));
        assertEquals(Boolean.TRUE, table.evaluate("""
                t => {
                  if(getComputedStyle(t).display!=='table'||!t.tHead||t.tHead.getBoundingClientRect().width<2)return false;
                  const p=t.parentElement,style=getComputedStyle(p),minimum=parseFloat(getComputedStyle(t).minWidth)||0;
                  const borders=parseFloat(style.borderLeftWidth)+parseFloat(style.borderRightWidth);
                  const scrollbar=p.scrollHeight>p.clientHeight?p.offsetWidth-p.clientWidth-borders:0;
                  const available=p.getBoundingClientRect().width-borders-parseFloat(style.paddingLeft)
                    -parseFloat(style.paddingRight)-scrollbar;
                  if(Math.abs(t.getBoundingClientRect().width-Math.max(available,minimum))>2)return false;
                  const heads=Array.from(t.tHead.rows[0].cells);
                  if(heads.some(h=>getComputedStyle(h).clipPath!=='none'||h.scrollWidth>h.clientWidth+2))return false;
                  return Array.from(t.tBodies).every(b=>Array.from(b.rows).every(r=>Array.from(r.cells).every((c,i)=>{
                    const a=c.getBoundingClientRect(),h=heads[i].getBoundingClientRect();
                    return getComputedStyle(c).display==='table-cell'&&Math.abs(a.x-h.x)<2&&Math.abs(a.width-h.width)<2
                      &&c.scrollWidth<=c.clientWidth+2;
                  })));
                }
                """), "real header/body geometry must preserve every column");
    }

    private static void settle(Page page) {
        page.waitForFunction("() => Array.from(document.querySelectorAll('[data-family] table')).every(t=>t.hasAttribute('data-atf-content-column-widths'))");
        page.evaluate("() => new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))");
    }

    private static final class Fixture implements AutoCloseable {
        private final Playwright playwright = BrowserResources.create(Playwright::create);
        private final Browser browser = BrowserResources.launch(playwright, new BrowserType.LaunchOptions().setHeadless(true));
        private final Page page = BrowserResources.newPage(browser);
        private final List<String> errors = new ArrayList<>();

        Fixture(boolean fallback) {
            page.onPageError(errors::add);
            page.setViewportSize(936, 900);
            String[][][] rows = {
                    {{"ASSERTION", "EXPECTED", "ACTUAL", "RESULT"}, {"Check", "OK", LONG, "PASS"}},
                    {{"ACTION", "TARGET", "EXPECTED", "OBSERVED", "TIME", "RESULT"}, {"Click", LONG, "OK", "OK", "1 ms", "PASS"}},
                    {{"FIELD", "VALUE"}, {LONG, "OK"}}, {{"FIELD", "VALUE"}},
                    {{"ASSERTION", "EXPECTED", "ACTUAL", "RESULT"}, {"Short check", "OK", "OK", "PASS"}},
                    {{"CHECK", "EXPECTED", "ACTUAL", "RESULT"}, {"Setup condition", "OK", LONG, "PASS"}}};
            String[] families = {"helm-validation-table", "atf-ui-action-table", "helm-trace-table", "future-column-family", "helm-validation-table", "atf-setup-evidence-table"};
            StringBuilder html = new StringBuilder("<!doctype html><html><head><style>")
                    .append(fallback ? AtfReportTheme.fallbackCss() : AtfReportTheme.css())
                    .append("body{margin:8px}.fixture{margin-bottom:12px;min-width:0}</style></head><body>");
            for (int i = 0; i < rows.length; i++) html.append("<section class='fixture' data-family='")
                    .append(i).append("'>").append(AtfReportMarkup.tableHtml(families[i], rows[i])).append("</section>");
            String dashboardTable = "<table><colgroup><col style='width:5%'><col style='width:80%'>"
                    + "<col style='width:5%'><col style='width:10%'></colgroup><thead><tr>"
                    + "<th>Name</th><th>Passed</th><th>Failed</th><th>Skipped</th></tr></thead><tbody><tr>"
                    + "<td>" + LONG + "</td><td>2</td><td>0</td><td>1</td></tr></tbody></table>";
            html.append("<section class='fixture atf-tag-scroll' data-family='tags'>").append(dashboardTable).append("</section>");
            for (String owner : List.of("category-container", "sysenv-container")) html
                    .append("<section class='fixture dashboard-view' data-family='").append(owner)
                    .append("'><div class='").append(owner).append("'><div class='card'><div class='card-body'>")
                    .append(dashboardTable).append("</div></div></div></section>");
            html.append("<script>").append(ReportAssetComposer.extentEvidenceTableLayoutScript()).append("</script></body></html>");
            page.setContent(html.toString());
            settle(page);
        }

        List<Locator> tables() { return LOCATORS.componentTables(page).all(); }

        public void close() {
            BrowserResources.finish(() -> assertTrue(errors.isEmpty(), errors::toString), browser, playwright);
        }
    }
}
