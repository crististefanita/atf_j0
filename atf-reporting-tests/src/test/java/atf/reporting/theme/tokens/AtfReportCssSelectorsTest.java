package atf.reporting.theme.tokens;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class AtfReportCssSelectorsTest {
    @Test
    void keepsSharedReportSurfacesTypedAndComposable() {
        assertAll(
                () -> assertEquals(".test-view", AtfReportCssSelectors.Surface.TEST_VIEW.selector()),
                () -> assertEquals("#test-view", AtfReportCssSelectors.Surface.TEST_VIEW.hash()),
                () -> assertEquals(".test-content-detail:visible",
                        AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.visible()),
                () -> assertEquals("[class~='test-list']",
                        AtfReportCssSelectors.Component.TEST_LIST.attributeSelector()),
                () -> assertEquals("table[class~='card']",
                        AtfReportCssSelectors.Component.CARD.attributeSelector("table")),
                () -> assertEquals(".test-list .test-item", AtfReportCssSelectors.descendant(
                        AtfReportCssSelectors.Component.TEST_LIST,
                        AtfReportCssSelectors.Component.TEST_ITEM)),
                () -> assertEquals("body", AtfReportCssSelectors.Surface.BODY.selector()));
    }

    @Test
    void keepsFallbackAndScenarioSelectorsTyped() {
        assertAll(
                () -> assertEquals(".tab", AtfReportCssSelectors.Fallback.TAB.selector()),
                () -> assertEquals(".tab.active", AtfReportCssSelectors.compound(
                        AtfReportCssSelectors.Fallback.TAB.selector(),
                        AtfReportCssSelectors.Fallback.ACTIVE.selector())),
                () -> assertEquals(".card[data-atf-scenario-id]>.card-header",
                        AtfReportCssSelectors.Component.SCENARIO_HEADER.selector()),
                () -> assertEquals(".card>.card-header",
                        AtfReportCssSelectors.directChild(".card", ".card-header")));
    }
}
