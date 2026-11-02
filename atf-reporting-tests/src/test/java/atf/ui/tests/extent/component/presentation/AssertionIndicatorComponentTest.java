package atf.ui.tests.extent.component.presentation;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.presentation.PresentationReportFixture;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** The rendered indicator changes state and remains distinct from the assertion verdict and sibling state. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssertionIndicatorComponentTest {
    private static final ReportPhaseAuditLocators LOCATORS = new ReportPhaseAuditLocators();
    @TempDir Path directory;
    private Playwright playwright;
    private Browser browser;
    @BeforeAll void start() { playwright = Playwright.create(); browser = playwright.chromium().launch(); }
    @AfterAll void stop() { if (browser != null) browser.close(); if (playwright != null) playwright.close(); }
    static Stream<Arguments> matrix() {
        return Stream.of(false, true).flatMap(fallback -> Stream.of(false, true)
                .flatMap(aggregate -> Stream.of("mouse", "Enter", "Space").map(input -> Arguments.of(fallback, aggregate, input))));
    }
    @ParameterizedTest @MethodSource("matrix")
    void indicatorIsVisibleAcrossOpenCloseAndKeyboard(boolean fallback, boolean aggregate, String input) {
        Path file = PresentationReportFixture.publish(directory.resolve("indicator-" + fallback + "-" + aggregate + "-" + input + ".html"), fallback, aggregate);
        try (var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1000, 850))) {
            page.setDefaultTimeout(5_000); page.setDefaultNavigationTimeout(30_000);
            page.navigate(file.toUri() + (fallback ? "" : "#test-view"));
            if (fallback) LOCATORS.testsTab(page, true).click();
            else {
                LOCATORS.firstOwner(page).click();
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            }
            var scenario = LOCATORS.scenarios(page, fallback).first();
            if (!fallback) LOCATORS.header(scenario).click();
            var phase = LOCATORS.phases(scenario).first();
            if (!fallback) LOCATORS.header(phase).click();
            var step = LOCATORS.assertionStep(phase).first();
            var header = fallback ? step.locator(":scope > summary") : LOCATORS.header(step);
            var glyph = fallback ? header.locator(":scope > .atf-disclosure-glyph") : header;
            verifyGlyph(glyph, false);
            var table = step.locator(fallback ? "table.assertions" : "table.helm-validation-table");
            assertThat(table).isHidden();
            toggle(header, input); verifyGlyph(glyph, true); assertThat(table).isVisible();
            assertThat(table.locator("tbody > tr").last()).isVisible();
            assertTrue(table.innerText().contains("Final original comparison"));
            page.addStyleTag(new Page.AddStyleTagOptions().setContent("#indicator-mutant::before{content:none!important}"));
            glyph.evaluate("element => element.setAttribute('id','indicator-mutant')");
            assertThrows(AssertionError.class, () -> verifyGlyph(glyph, true), "ARIA/open and an intact table cannot replace a visible indicator");
            assertThat(table).isVisible();
            glyph.evaluate("element => element.removeAttribute('id')");
            toggle(header, input); verifyGlyph(glyph, false); assertThat(table).isHidden();
            if (aggregate && fallback) assertNull(LOCATORS.scenarios(page, true).nth(1).locator("details[data-atf-node-role=assertion-step]").getAttribute("open"));
        }
    }
    private static void toggle(Locator header, String input) { if (input.equals("mouse")) header.click(); else header.press(input); }
    private static void verifyGlyph(Locator glyph, boolean open) {
        assertEquals(1, glyph.count(), "one rendered disclosure indicator"); assertThat(glyph).isVisible();
        var visible = (List<?>) glyph.evaluate("""
                e => {const s=getComputedStyle(e),p=getComputedStyle(e,'::before'),r=e.getBoundingClientRect();
                  return [p.content.replaceAll('"','').replaceAll("'",''),r.width,r.height,p.color,s.visibility,p.visibility,p.display];}
                """);
        assertTrue((open ? List.of("−", "-") : List.of("＋", "+")).contains(visible.get(0)), "visible open/closed glyph: " + visible);
        assertTrue(((Number) visible.get(1)).doubleValue() >= 14); assertTrue(((Number) visible.get(2)).doubleValue() >= 14);
        assertFalse(visible.get(3).equals("rgba(0, 0, 0, 0)")); assertEquals("visible", visible.get(4)); assertEquals("visible", visible.get(5));
        assertFalse(visible.get(6).equals("none"));
    }
}
