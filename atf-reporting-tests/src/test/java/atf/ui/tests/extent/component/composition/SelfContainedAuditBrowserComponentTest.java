package atf.ui.tests.extent.component.composition;

import atf.reporting.support.composition.SelfContainedAuditFixture;
import atf.reporting.support.composition.SelfContainedAuditOracle;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.*;
import java.nio.file.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

/** Browser proof of original text, closed neighbors and readable inline component/UI-shaped evidence. */
@AtfComponentTest
class SelfContainedAuditBrowserComponentTest {
    static Stream<Arguments> styles() { return Stream.of(false, true).flatMap(extent -> Stream.of(false, true).map(ui -> Arguments.of(extent, ui))); }
    @ParameterizedTest @MethodSource("styles")
    void nestedFullValuesKeepTheirLiteralTextAndKeyboardBehavior(boolean extent, boolean ui) throws Exception {
        var file = SelfContainedAuditFixture.publish(extent, ui, true, "browser");
        SelfContainedAuditOracle.verify(file, extent, ui, true);
        try (var playwright = Playwright.create(); var browser = playwright.chromium().launch();
             var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1100, 850))) {
            var errors = new ArrayList<String>(); page.onPageError(errors::add); page.setDefaultTimeout(5000);
            var isolated = file.resolveSibling("html-only").resolve(file.getFileName());
            Files.createDirectories(isolated.getParent()); Files.copy(file, isolated, StandardCopyOption.REPLACE_EXISTING);
            page.route("http://**/*", Route::abort); page.route("https://**/*", Route::abort);
            page.navigate(isolated.toUri() + (extent ? "#test-view" : ""));
            if (extent) {
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes").setExact(true)).click();
            } else page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Tests").setExact(true)).click();
            var observation = page.locator("[data-atf-node-role=observation] > details:visible").first();
            assertNull(observation.getAttribute("open")); observation.locator(":scope > summary").press("Enter");
            var json = observation.locator("details").first(); var context = observation.locator("details").nth(1);
            assertNull(json.getAttribute("open")); assertNull(context.getAttribute("open"));
            var summary = context.locator(":scope > summary"); summary.press(" ");
            var text = context.locator("pre"); assertEquals(SelfContainedAuditFixture.narrative(0), text.textContent());
            text.press("End"); page.waitForFunction("e => e.scrollTop + e.clientHeight >= e.scrollHeight - 1", text.elementHandle());
            assertNull(json.getAttribute("open"), "Opening context does not expand the sibling JSON");
            page.setViewportSize(900, 850);
            page.waitForFunction("e => e.querySelector('pre').getBoundingClientRect().top >= e.querySelector('summary').getBoundingClientRect().bottom - 1", context.elementHandle());
            assertEquals(true, context.evaluate("e => e.querySelector('pre').getBoundingClientRect().top >= e.querySelector('summary').getBoundingClientRect().bottom - 1"),
                    "Summary and body do not overlap in the same layout snapshot");
            summary.press("Enter"); assertNull(context.getAttribute("open")); summary.press(" ");
            assertEquals(SelfContainedAuditFixture.narrative(0), text.textContent());
            assertTrue(errors.isEmpty(), errors.toString());
        }
    }
}
