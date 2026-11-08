package atf.ui.tests.extent.component.navigation;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.TestReportPaths;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real report navigation keeps one semantic and visible selection per transition. */
@AtfComponentTest
@ReportBrowserTest
class NavigationSelectionBrowserComponentTest {
    private static final List<NavigationTarget> TARGETS = List.of(
            new NavigationTarget("nav-test", "test-view", "Tests"),
            new NavigationTarget("nav-category", "category-view", "Category"),
            new NavigationTarget("nav-fails", "atf-fails-view", "Fails"),
            new NavigationTarget("nav-overview", "dashboard-view", "Overview"));

    @Test
    void successiveTabsExposeOneAccessibleCurrentOwner() throws Exception {
        Path report = TestReportPaths.report("extent-navigation-selection-component.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("NavigationOwner", "navigation selection", List.of("component"));
            scenario.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        try (var playwright = BrowserResources.open(Playwright::create);
             var browser = BrowserResources.open(() -> BrowserResources.launch(playwright.value()));
             var pageHandle = BrowserResources.open(() -> BrowserResources.newPage(browser.value()))) {
            try {
                Page page = pageHandle.value();
                page.setDefaultTimeout(10000);
                page.route("http://**/*", Route::abort);
                page.route("https://**/*", Route::abort);
                page.setContent(Files.readString(report));
                waitForSelection(page, TARGETS.getLast());
                assertSelection(page, TARGETS.getLast());

                for (NavigationTarget target : TARGETS) {
                    Locator link = page.getByRole(AriaRole.LINK,
                            new Page.GetByRoleOptions().setName(target.label()).setExact(true));
                    assertEquals(1, link.count(), "Icon-only navigation must expose its accessible label");
                    link.click();
                    waitForSelection(page, target);
                    assertSelection(page, target);
                    assertEquals("#" + target.viewId(), page.evaluate("() => location.hash"),
                            "The address bar must retain the selected report view");
                }
            } catch (Exception | Error failure) {
                BrowserResources.recordFailure(failure);
                throw failure;
            }
        }
    }

    private static void waitForSelection(Page page, NavigationTarget target) {
        try {
            page.waitForFunction("target => {"
                            + "const link=document.getElementById(target.linkId);"
                            + "const view=document.getElementById(target.viewId);"
                            + "return link?.classList.contains('active')"
                            + " && link.getAttribute('aria-current')==='page'"
                            + " && view && !view.classList.contains('hide')"
                            + " && !view.classList.contains('d-none');"
                            + "}",
                    java.util.Map.of("linkId", target.linkId(), "viewId", target.viewId()));
        } catch (RuntimeException failure) {
            Object state = page.evaluate("() => JSON.stringify({hash:location.hash,active:[...document.querySelectorAll('.side-nav a.active')].map(e=>e.id),"
                    + "current:[...document.querySelectorAll('.side-nav a[aria-current=page]')].map(e=>e.id),"
                    + "views:[...document.querySelectorAll('.view')].map(e=>[e.id,e.className])})");
            throw new AssertionError("Selection did not settle for " + target + "; " + state, failure);
        }
    }

    private static void assertSelection(Page page, NavigationTarget target) {
        assertEquals(1, page.locator(".side-nav a.active").count(), "Exactly one tab link is active");
        assertEquals(1, page.locator(".side-nav a[aria-current='page']").count(), "Exactly one tab is current");
        assertEquals(target.linkId(), page.locator(".side-nav a.active").getAttribute("id"));
        assertEquals("page", page.locator(".side-nav a[aria-current='page']").getAttribute("aria-current"));
        assertEquals(1, page.locator(".view:not(.hide):not(.d-none)").count(),
                "Exactly one report view must be rendered visible");
        assertEquals(target.viewId(), page.locator(".view:not(.hide):not(.d-none)").getAttribute("id"));
        Object hidden = page.locator("#" + target.viewId()).evaluate("element => element.classList.contains('hide')"
                + " || element.classList.contains('d-none')");
        Object state = page.evaluate("() => JSON.stringify({hash:location.hash,active:[...document.querySelectorAll('.side-nav a.active')].map(e=>e.id),"
                + "current:[...document.querySelectorAll('.side-nav a[aria-current=page]')].map(e=>e.id),"
                + "views:[...document.querySelectorAll('.view')].map(e=>[e.id,e.className])})");
        assertFalse((Boolean) hidden, "The selected tab and visible report view must share the same owner: "
                + target + "; class=" + page.locator("#" + target.viewId()).getAttribute("class") + "; " + state);
    }

    private record NavigationTarget(String linkId, String viewId, String label) { }
}
