package atf.ui.tests.extent.support.inspection.table;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.ui.contract.ReportUiContract.Tab;
import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises actual publications and every visible data table across the report navigation. */
public final class PublishedEvidenceTableProbe {
    private static final PublishedEvidenceTableLocators LOCATORS = new PublishedEvidenceTableLocators();

    private PublishedEvidenceTableProbe() { }

    public static void verify(PublishedEvidenceTableFixture.Profile profile, boolean fallback) {
        Path report = PublishedEvidenceTableFixture.publish(profile, fallback);
        try (var fixture = new BrowserFixture(report, fallback)) {
            try {
                Page page = fixture.page;
                List<String> original = PublishedEvidenceTableOracle.contents(page);
                PublishedEvidenceTableOracle.verifyInventory(page, profile);
                for (int width : List.of(1803, 1280, 991, 719, 720, 721, 591, 392)) {
                    page.setViewportSize(width, 1000);
                    settle(page);
                    PublishedEvidenceTableOracle.verify(page, profile + "; viewport=" + width);
                    PublishedEvidenceTableOracle.verifyShortAssertions(page);
                    PublishedEvidenceTableOracle.verifyInventory(page, profile);
                    assertEquals(original, PublishedEvidenceTableOracle.contents(page), "resize cannot change evidence");
                }
                page.setViewportSize(1280, 1000);
                for (int width : List.of(591, 392)) {
                    LOCATORS.panel(page, fallback).evaluateAll("(items,w)=>items.forEach(e=>e.style.width=w+'px')", width);
                    settle(page);
                    PublishedEvidenceTableOracle.verify(page, profile + "; nested panel=" + width);
                    assertEquals(original, PublishedEvidenceTableOracle.contents(page));
                }
                LOCATORS.panel(page, fallback).evaluateAll("items=>items.forEach(e=>e.style.removeProperty('width'))");
                var summary = LOCATORS.summaries(page).first();
                summary.press("Enter");
                assertEquals(Boolean.FALSE, summary.evaluate("s=>s.parentElement.open"));
                summary.press("Space");
                assertEquals(Boolean.TRUE, summary.evaluate("s=>s.parentElement.open"));
                settle(page);
                PublishedEvidenceTableOracle.verify(page, "keyboard reopen");
                assertEquals(original, PublishedEvidenceTableOracle.contents(page));
                page.setViewportSize(1803, 1000);
                for (Tab tab : List.of(Tab.OVERVIEW, Tab.CATEGORIES, Tab.FAILS)) {
                    page.setViewportSize(1803, 1000);
                    LOCATORS.tab(page, fallback, tab).click();
                    if (!fallback && tab == Tab.CATEGORIES) LOCATORS.firstCategory(page).click();
                    if (!fallback && tab == Tab.FAILS) LOCATORS.firstFailure(page).click();
                    if (!fallback && LOCATORS.expandAll(page).count() > 0) LOCATORS.expandAll(page).click();
                    openDetails(page);
                    for (int width : List.of(1803, 1280, 991, 761, 760, 591, 392)) {
                        page.setViewportSize(width, 1000);
                        settle(page);
                        PublishedEvidenceTableOracle.verifyView(page, fallback, tab);
                        if (width == 1803 || width == 392) page.screenshot(new Page.ScreenshotOptions().setPath(report.resolveSibling(
                                report.getFileName() + "-" + tab.name().toLowerCase(Locale.ROOT) + "-" + width + ".png")));
                    }
                }
                LOCATORS.tab(page, fallback, Tab.TESTS).click();
                if (!fallback) LOCATORS.expandAll(page).click();
                openDetails(page);
                PublishedEvidenceTableOracle.verify(page, "return to Tests");
                assertEquals(original, PublishedEvidenceTableOracle.contents(page), "tab navigation cannot lose or change evidence");
                PublishedEvidenceTableOracle.verifyShortAssertions(page);
                PublishedEvidenceTableOracle.verifyInventory(page, profile);
                if (!fallback) {
                    PublishedEvidenceTableOracle.verifyDerivedFailures(page, profile);
                    LOCATORS.tab(page, false, Tab.OVERVIEW).click();
                    LOCATORS.tab(page, false, Tab.TESTS).click();
                    LOCATORS.expandAll(page).click();
                    openDetails(page);
                    PublishedEvidenceTableOracle.verifyDerivedFailures(page, profile);
                    PublishedEvidenceTableOracle.verify(page, "repeat navigation is idempotent");
                    assertEquals(original, PublishedEvidenceTableOracle.contents(page), "canonical published evidence remains unchanged");
                }
                page.screenshot(new Page.ScreenshotOptions().setPath(report.resolveSibling(report.getFileName() + "-tests.png")));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
        System.out.println("PUBLISHED_TABLE_AUDIT=" + report);
    }

    public static void verifyNegative(boolean fallback) {
        Path report = PublishedEvidenceTableFixture.publish(PublishedEvidenceTableFixture.Profile.UI_API, fallback);
        try (var fixture = new BrowserFixture(report, fallback)) {
            try {
                Page page = fixture.page;
                List<String> original = PublishedEvidenceTableOracle.contents(page);
                for (String defect : List.of("gap", "header", "stacked", "clipped", "last-row", "changed-value", "lost-result", "gutter",
                        "unbounded", "lost-table", "lost-head", "long-pass", "long-value", "nested-clip")) {
                    Locator table = LOCATORS.assertions(page).all().stream()
                            .filter(t -> LOCATORS.rows(t).count() == (List.of("clipped", "long-pass", "long-value", "nested-clip")
                                    .contains(defect) ? 40 : 12))
                            .findFirst().orElseThrow();
                    try (var mutation = new PublishedEvidenceTableMutation(table)) {
                        table.evaluate("""
                                (t,defect)=>{
                                  if(defect==='gap')t.style.setProperty('width','50%','important');
                                  if(defect==='header')t.tHead.style.setProperty('clip-path','inset(50%)','important');
                                  if(defect==='stacked')Array.from(t.tBodies[0].rows[0].cells)
                                    .forEach(c=>c.style.setProperty('display','block','important'));
                                  if(defect==='clipped'){
                                    const c=t.tBodies[0].rows[0].cells[1];
                                    c.style.setProperty('white-space','nowrap','important');
                                    c.style.setProperty('overflow','hidden','important');
                                  }
                                  if(defect==='last-row')t.tBodies[0].rows[t.tBodies[0].rows.length-1]
                                    .style.setProperty('display','none','important');
                                  if(defect==='changed-value')t.tBodies[0].rows[0].cells[1].textContent='CORRUPTED';
                                  if(defect==='lost-result')Array.from(t.rows).forEach(r=>r.lastElementChild.remove());
                                  if(defect==='gutter')t.parentElement.style.setProperty('scrollbar-gutter','stable','important');
                                  if(defect==='unbounded')t.parentElement.style.setProperty('max-height','none','important');
                                  if(defect==='lost-table')t.remove();
                                  if(defect==='lost-head')t.tHead.remove();
                                  if(defect==='long-pass')t.tBodies[0].rows[0].cells[3].textContent='PASS';
                                  if(defect==='long-value')t.tBodies[0].rows[0].cells[1].textContent='CORRUPTED';
                                  if(defect==='nested-clip'){
                                    const c=t.tBodies[0].rows[0].cells[1],child=document.createElement('div');
                                    child.textContent=c.textContent;child.style.cssText='width:24px;white-space:nowrap;overflow:hidden';
                                    c.replaceChildren(child);
                                  }
                                }
                                """, defect);
                        assertThrows(AssertionError.class, () -> {
                            PublishedEvidenceTableOracle.verify(page, defect);
                            PublishedEvidenceTableOracle.verifyShortAssertions(page);
                            PublishedEvidenceTableOracle.verifyInventory(page, PublishedEvidenceTableFixture.Profile.UI_API);
                        }, "the oracle must reject " + defect + " with unchanged layout markers");
                    }
                    settle(page);
                    PublishedEvidenceTableOracle.verify(page, "restored " + defect);
                    PublishedEvidenceTableOracle.verifyInventory(page, PublishedEvidenceTableFixture.Profile.UI_API);
                    assertEquals(original, PublishedEvidenceTableOracle.contents(page), "negative controls restore the published evidence");
                }
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void openDetails(Page page) {
        int opened = 0;
        while (LOCATORS.closedDetails(page).count() > 0) {
            assertTrue(opened++ < 256, "published disclosures must converge to a fully opened state");
            var disclosure = LOCATORS.firstClosedDisclosure(page);
            LOCATORS.summary(disclosure).press("Enter");
            assertEquals(Boolean.TRUE, disclosure.evaluate("d => d.open"), "keyboard opens the selected evidence");
            settle(page);
        }
        settle(page);
    }

    private static void settle(Page page) {
        page.evaluate("""
                async()=>{
                  const frame=()=>new Promise(r=>requestAnimationFrame(r));
                  await frame();
                  await Promise.all(document.getAnimations().filter(a=>a.effect.getComputedTiming().endTime<2000)
                    .map(a=>a.finished.catch(()=>{})));
                  await frame();await frame();
                }
                """);
    }

    public static void verifyNativeVisualDefects() {
        Path report = PublishedEvidenceTableFixture.publish(PublishedEvidenceTableFixture.Profile.UI, false);
        try (var fixture = new BrowserFixture(report, false)) {
            try {
                Page page = fixture.page;
                page.setViewportSize(392, 1000);
                LOCATORS.tab(page, false, Tab.FAILS).click();
                LOCATORS.firstFailure(page).click();
                settle(page);
                PublishedEvidenceTableOracle.verifyView(page, false, Tab.FAILS);
                Locator view = LOCATORS.view(page, false, Tab.FAILS);
                String style = Objects.requireNonNullElse(view.getAttribute("style"), "");
                try {
                    view.evaluate("e=>{e.style.setProperty('left','0','important');"
                            + "e.style.setProperty('width','100%','important');e.style.setProperty('max-width','100%','important');}");
                    settle(page);
                    assertThrows(AssertionError.class, () -> PublishedEvidenceTableOracle.verifyView(page, false, Tab.FAILS),
                            "document overflow alone cannot detect content hidden under the fixed navigation");
                } finally {
                    view.evaluate("(e,s)=>{if(s)e.setAttribute('style',s);else e.removeAttribute('style');}", style);
                }
                settle(page);
                PublishedEvidenceTableOracle.verifyView(page, false, Tab.FAILS);
                LOCATORS.tab(page, false, Tab.CATEGORIES).click();
                LOCATORS.firstCategory(page).click();
                settle(page);
                PublishedEvidenceTableOracle.verifyView(page, false, Tab.CATEGORIES);
                PublishedEvidenceTableOracle.dataTables(page).getFirst().evaluate(
                        """
                        t=>{
                          const h=t.tHead.rows[0].cells[0],label=document.createElement('span');
                          label.textContent=h.textContent;
                          label.style.cssText='display:inline-block;width:20px;white-space:normal;overflow-wrap:anywhere';
                          h.replaceChildren(label);
                        }
                        """);
                assertThrows(AssertionError.class, () -> PublishedEvidenceTableOracle.verify(page, "wrapped header"));
                assertTrue(PublishedEvidenceTableMeasurements.geometry(page).get("tables").toString()
                        .contains("technical header wraps"), "the oracle must detect word wrapping, not only scroll clipping");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static final class BrowserFixture implements AutoCloseable {
        private final Playwright playwright = BrowserResources.create(Playwright::create);
        private final Browser browser = BrowserResources.launch(playwright, new BrowserType.LaunchOptions().setHeadless(true));
        private final Page page = BrowserResources.newPage(browser);
        private final List<String> errors = new ArrayList<>();
        private final boolean fallback;

        private BrowserFixture(Path report, boolean fallback) {
            this.fallback = fallback;
            page.setViewportSize(1803, 1000);
            page.setDefaultTimeout(8_000);
            page.onPageError(errors::add);
            page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
            open();
        }

        private void open() {
            if (fallback) LOCATORS.tab(page, true, Tab.TESTS).click();
            else {
                LOCATORS.firstOwner(page).click();
                LOCATORS.expandAll(page).click();
            }
            openDetails(page);
        }

        @Override public void close() {
            BrowserResources.finish(() -> assertTrue(errors.isEmpty(), errors::toString), browser, playwright);
        }
    }
}

