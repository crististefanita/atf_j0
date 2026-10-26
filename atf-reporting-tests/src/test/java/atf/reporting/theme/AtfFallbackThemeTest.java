package atf.reporting.theme;
import atf.testkit.AtfUnitTest;
import atf.reporting.theme.tokens.AtfFallbackTheme;
import atf.reporting.theme.tokens.AtfReportThemeTokens;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@AtfUnitTest
class AtfFallbackThemeTest {
    @Test
    void fallbackThemeOwnsTheRendererNeutralVisualContract() {
        String css = AtfReportTheme.fallbackCss();

        assertAll(
                () -> assertTrue(css.contains("--atf-dark:")),
                () -> assertTrue(css.contains(".tabs{")),
                () -> assertTrue(css.contains(".tab.active{")),
                () -> assertTrue(css.contains(".phase{")),
                () -> assertTrue(css.contains(".request,.validation{")),
                () -> assertTrue(css.contains("pre.json-code{")),
                () -> assertTrue(css.contains(".status-pass{")),
                () -> assertTrue(css.contains(".status-fail{")),
                () -> assertTrue(css.contains(".status-warning{")),
                () -> assertTrue(css.contains(".status-skip{")),
                () -> assertTrue(css.contains(".summary{")));
    }

    @Test
    void fallbackThemeReusesTheSharedFontAndColorTokens() {
        String css = AtfFallbackTheme.css();

        assertAll(
                () -> assertTrue(css.contains("font-family:" + AtfReportThemeTokens.FONT_STACK)),
                () -> assertTrue(css.contains("background:var(--atf-dark)")),
                () -> assertTrue(css.contains("color:var(--atf-text)")),
                () -> assertTrue(css.contains("var(--atf-pass)")),
                () -> assertTrue(css.contains("var(--atf-fail)")));
    }
}
