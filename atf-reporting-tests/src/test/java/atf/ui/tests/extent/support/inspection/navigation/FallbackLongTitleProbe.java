package atf.ui.tests.extent.support.inspection.navigation;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.support.navigation.FallbackLongTitleFixture;
import atf.ui.tests.extent.support.locators.navigation.FallbackLongTitleLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.ArrayList;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Measures rendered text fragments; a hidden or clipped title cannot pass this oracle. */
public final class FallbackLongTitleProbe {
    private static final FallbackLongTitleLocators LOCATORS = new FallbackLongTitleLocators();
    private FallbackLongTitleProbe() { }

    public static void verify(int width, boolean regression) throws Exception {
        var fixture = FallbackLongTitleFixture.publish();
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value(), new Browser.NewContextOptions().setViewportSize(width, 900)))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(4000);
                var errors = new ArrayList<String>();page.onPageError(errors::add);
                page.navigate(fixture.path().toUri().toString());LOCATORS.tests(page).click();
                assertThat(LOCATORS.links(page)).hasCount(2);
                assertTrue(LOCATORS.links(page).first().innerText().contains(FallbackLongTitleFixture.TITLE));
                assertTrue(LOCATORS.links(page).first().innerText().contains(FallbackLongTitleFixture.CLASS_NAME));
                var first = LOCATORS.scenario(page, fixture.firstId());
                assertEquals(FallbackLongTitleFixture.TITLE, LOCATORS.heading(first).innerText());
                if (regression) {
                    page.addStyleTag(new Page.AddStyleTagOptions().setContent(".test-link{white-space:nowrap!important}"));
                    var failure = assertThrows(AssertionError.class, () -> verifyGeometry(page));
                    assertTrue(failure.getMessage().contains("title containment"));
                    return;
                }
                verifyGeometry(page);
                LOCATORS.links(page).first().press("Enter");
                page.waitForURL(url -> fixture.firstId().equals(java.net.URI.create(url).getFragment()));
                LOCATORS.assertionSummary(first).press("Space");
                assertThat(LOCATORS.assertionRows(first)).hasCount(1);
                assertThat(LOCATORS.assertionRows(first)).isVisible();
                assertTrue(LOCATORS.assertionRows(first).innerText().contains("original evidence"));
                LOCATORS.assertionSummary(first).press("Enter");assertThat(LOCATORS.assertionRows(first)).not().isVisible();
                LOCATORS.links(page).last().press("Enter");
                page.waitForURL(url -> fixture.siblingId().equals(java.net.URI.create(url).getFragment()));
                var sibling = LOCATORS.scenario(page, fixture.siblingId());
                LOCATORS.assertionSummary(sibling).press("Enter");
                assertTrue(LOCATORS.assertionRows(sibling).innerText().contains("sibling evidence"));
                LOCATORS.links(page).first().click();page.reload();
                assertEquals(fixture.firstId(), java.net.URI.create(page.url()).getFragment());
                assertThat(LOCATORS.assertionRows(first)).not().isVisible();
                verifyGeometry(page);
                assertTrue(errors.isEmpty(), errors::toString);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void verifyGeometry(Page page) {
        assertContained(LOCATORS.links(page).first());
        assertContained(LOCATORS.heading(LOCATORS.firstScenario(page)));
        double asideRight = LOCATORS.sidebar(page).boundingBox().x + LOCATORS.sidebar(page).boundingBox().width;
        var content = LOCATORS.content(page).boundingBox();
        assertTrue(asideRight <= content.x + 1 || LOCATORS.sidebar(page).boundingBox().y
                + LOCATORS.sidebar(page).boundingBox().height <= content.y + 1, "Sidebar and article remain disjoint");
        assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
                "Long titles must not create page-wide horizontal overflow");
    }

    private static void assertContained(Locator element) {
        boolean contained = (Boolean) element.evaluate("""
                element => {
                  const box=element.getBoundingClientRect(), style=getComputedStyle(element);
                  const walker=document.createTreeWalker(element,NodeFilter.SHOW_TEXT);
                  const rects=[];let node;
                  while(node=walker.nextNode()) {
                    if(!node.textContent.trim()) continue;
                    const range=document.createRange();range.selectNodeContents(node);
                    rects.push(...range.getClientRects());
                  }
                  return style.visibility==='visible' && Number(style.opacity)>0 && rects.length>0 &&
                    rects.every(r=>r.left>=box.left-1 && r.right<=box.right+1 && r.top>=box.top-1 && r.bottom<=box.bottom+1);
                }
                """);
        assertTrue(contained, "Rendered title containment failed: " + element.innerText().substring(0, 35));
    }
}
