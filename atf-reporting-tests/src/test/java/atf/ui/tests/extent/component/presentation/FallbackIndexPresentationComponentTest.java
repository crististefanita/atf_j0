package atf.ui.tests.extent.component.presentation;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.presentation.PresentationReportFixture;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Path;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Checks real text contrast, independent status badges, complete labels and keyboard navigation. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ReportBrowserTest
class FallbackIndexPresentationComponentTest {
    @TempDir Path directory;
    private Playwright playwright;
    private Browser browser;
    @BeforeAll void start() { playwright = BrowserResources.create(Playwright::create); browser = BrowserResources.launch(playwright); }
    @AfterAll void stop() {
        BrowserResources.closeAll(browser, playwright);
    }
    @ParameterizedTest @ValueSource(ints = {1280, 392})
    void statusDoesNotRepaintTheIndexOrConcealLabels(int width) {
        Path file = PresentationReportFixture.publish(directory.resolve("index-" + width + ".html"), true, true);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser, new Browser.NewContextOptions().setViewportSize(width, 900)))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5_000); page.setDefaultNavigationTimeout(30_000);
                page.navigate(file.toUri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Tests").setExact(true)).click();
                var links = page.locator("#tests aside .test-link"); assertEquals(3, links.count());
                for (int i = 0; i < 3; i++) {
                    var link = links.nth(i); assertThat(link).isVisible();
                    assertEquals(PresentationReportFixture.CLASS_NAME, link.locator("small").innerText());
                    assertTrue(link.innerText().contains(PresentationReportFixture.TITLE + i));
                    assertEquals(1, link.locator(":scope > .status").count(), "one separate status badge per original link");
                    assertEquals(List.of("Pass", "Fail", "Skip").get(i), link.locator(":scope > .status").innerText());
                    verifyPresentation(link);
                    link.press("Shift+Tab"); page.keyboard().press("Tab");
                    assertEquals(Boolean.TRUE, link.evaluate("e => e === document.activeElement"));
                    assertEquals(Boolean.TRUE, link.evaluate("e => getComputedStyle(e).outlineStyle !== 'none' && parseFloat(getComputedStyle(e).outlineWidth) >= 2"));
                    String target = link.getAttribute("href").substring(1);
                    link.press("Enter");
                    page.waitForCondition(() -> target.equals(URI.create(page.url()).getFragment()));
                    assertThat(page.locator("article[id='" + link.getAttribute("href").substring(1) + "'] h2"))
                            .hasText(PresentationReportFixture.TITLE + i);
                }
                var link = links.first();
                link.evaluate("e => e.classList.add('status','status-pass')");
                assertThrows(AssertionError.class, () -> verifyPresentation(link), "the old whole-link badge must be refused");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static void verifyPresentation(Locator link) {
        assertFalse(List.of(link.getAttribute("class").split("\\s+")).contains("status"));
        var values = link.evaluate("""
                e => {
                  const colors=s=>s.match(/[\\d.]+/g).map(Number);
                  const rgb=c=>c.slice(0,3).map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4});
                  const lum=c=>{const v=rgb(c);return .2126*v[0]+.7152*v[1]+.0722*v[2]};
                  let parent=e,bg; do {bg=colors(getComputedStyle(parent).backgroundColor);parent=parent.parentElement}
                  while(bg.length===4&&bg[3]===0&&parent);
                  const ratio=n=>{const a=lum(colors(getComputedStyle(n).color)),b=lum(bg);return (Math.max(a,b)+.05)/(Math.min(a,b)+.05)};
                  const c=e.querySelector('small');const s=getComputedStyle(e);
                  return [ratio(e),ratio(c),parseFloat(s.fontSize),e.scrollWidth-e.clientWidth,
                    s.whiteSpace==='normal',getComputedStyle(c).whiteSpace==='normal'];
                }
                """);
        var v = (List<?>) values;
        assertTrue(((Number) v.get(0)).doubleValue() >= 4.5, "scenario text contrast");
        assertTrue(((Number) v.get(1)).doubleValue() >= 4.5, "complete class text contrast");
        assertTrue(((Number) v.get(2)).doubleValue() >= 14, "index must not inherit the compact status font");
        assertTrue(((Number) v.get(3)).doubleValue() <= 1, "labels stay inside the link");
        assertEquals(Boolean.TRUE, v.get(4)); assertEquals(Boolean.TRUE, v.get(5));
    }
}
