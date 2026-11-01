package atf.ui.tests.extent.component.inspection.disclosure;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.json.JsonEvidenceFixture;
import atf.ui.tests.extent.support.locators.disclosure.ApiValidationDisclosureLocators;
import atf.ui.tests.extent.support.locators.disclosure.SiblingDisclosureLocators;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Real publishers, neutral mock evidence and the active owner; hidden DOM cannot satisfy the oracle. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SiblingDisclosureStateComponentTest {
    private static final ApiValidationDisclosureLocators DOM = new ApiValidationDisclosureLocators();
    private static final SiblingDisclosureLocators CONTROLS = new SiblingDisclosureLocators();
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void start() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }
    @AfterAll void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
    static Stream<Arguments> matrix() {
        return Stream.of(false, true).flatMap(fallback -> Stream.of(false, true).flatMap(aggregate ->
                Stream.of("mouse", "Enter", "Space").map(action -> Arguments.of(fallback, aggregate, action))));
    }

    @ParameterizedTest @MethodSource("matrix")
    void localControlsPreserveTheOtherScenarioAndBulkControlsRemainExplicit(boolean fallback, boolean aggregate, String action)
            throws Exception {
        var report = JsonEvidenceFixture.publish(fallback, aggregate);
        try (var page = browser.newPage()) {
            page.setDefaultTimeout(5_000);
            page.setDefaultNavigationTimeout(30_000);
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(report.toUri() + "#test-view", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            DOM.selectTests(page, fallback);
            var subject = DOM.scenario(page, fallback, JsonEvidenceFixture.SUBJECT);
            if (!fallback) CONTROLS.expandAll(page);
            else DOM.summary(DOM.disclosure(DOM.table(DOM.validation(subject)), true), true).click();
            CONTROLS.summary(CONTROLS.response(subject)).click();
            verifyVisibleEvidence(subject, fallback);
            var before = CONTROLS.states(subject, fallback);
            if (aggregate) {
                for (var title : List.of("JSON neighbor before", "JSON neighbor after")) {
                    var neighbor = DOM.scenario(page, fallback, title);
                    var control = fallback ? DOM.summary(DOM.disclosure(DOM.table(DOM.validation(neighbor)), true), true)
                            : CONTROLS.header(neighbor);
                    toggle(control, action);
                    verifyVisibleEvidence(subject, fallback);
                    assertEquals(before, CONTROLS.states(subject, fallback), "neighbor changed subject state");
                    toggle(control, action);
                    verifyVisibleEvidence(subject, fallback);
                }
            } else {
                var control = DOM.summary(DOM.disclosure(DOM.table(DOM.validation(subject)), fallback), fallback);
                toggle(control, action);
                assertThat(DOM.table(DOM.validation(subject))).isHidden();
                toggle(control, action);
                verifyVisibleEvidence(subject, fallback);
            }
            if (!fallback) {
                CONTROLS.collapseAll(page);
                assertThat(DOM.table(DOM.validation(subject))).isHidden();
                CONTROLS.expandAll(page);
                verifyVisibleEvidence(subject, false);
            }
            assertTrue(errors.isEmpty(), errors.toString());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oracleRejectsInvisibleBodyEvenWhenExpandedAttributesAndAllValuesRemain(boolean aggregate) throws Exception {
        var report = JsonEvidenceFixture.publish(false, aggregate);
        try (var page = browser.newPage()) {
            page.setDefaultTimeout(5_000);
            page.setDefaultNavigationTimeout(30_000);
            page.navigate(report.toUri() + "#test-view", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            DOM.selectTests(page, false);
            CONTROLS.expandAll(page);
            var subject = DOM.scenario(page, false, JsonEvidenceFixture.SUBJECT);
            CONTROLS.summary(CONTROLS.response(subject)).click();
            verifyVisibleEvidence(subject, false);
            CONTROLS.hideBodyKeepingExpandedMetadata(subject);
            assertThrows(AssertionError.class, () -> verifyVisibleEvidence(subject, false));
        }
    }

    private static void toggle(Locator control, String action) {
        if (action.equals("mouse")) control.click(); else control.press(action);
    }
    private static void verifyVisibleEvidence(Locator subject, boolean fallback) {
        var table = DOM.table(DOM.validation(subject));
        assertEquals(1, table.count(), "one original comparison table at the active subject");
        assertThat(table).isVisible();
        assertThat(CONTROLS.codes(subject).first()).isVisible();
        var rows = DOM.rows(table);
        assertEquals(3, rows.count());
        for (int index = 0; index < 3; index++) {
            var cells = DOM.cells(rows.nth(index));
            assertEquals(index == 2 ? fallback ? "Pass" : "✅" : fallback ? "Fail" : "❌", cells.last().innerText().trim());
            assertTrue(cells.nth(1).innerText().length() <= 240);
            assertTrue(cells.nth(2).innerText().length() <= 240);
        }
        assertEquals(JsonEvidenceFixture.EXPECTED, DOM.cells(rows.first()).nth(1).innerText().trim());
        assertEquals(JsonEvidenceFixture.ACTUAL, DOM.cells(rows.first()).nth(2).innerText().trim());
        rows.last().scrollIntoViewIfNeeded();
        assertThat(rows.last()).isVisible();
        assertEquals("JSON null · final comparison", DOM.cells(rows.last()).first().innerText().trim());
    }
}
