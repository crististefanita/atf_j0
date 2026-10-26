package atf.reporting.theme;
import atf.testkit.AtfUnitTest;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportFailsLayoutCssTest {
    @Test
    void failsProjectionOwnsAStableNoWrapTwoPaneContract() {
        String css = AtfReportFailsLayoutCss.css();

        assertAll(
                () -> assertTrue(css.contains("display:flex!important;flex-direction:row!important;flex-wrap:nowrap!important")),
                () -> assertTrue(css.contains("flex:0 0 32%!important;max-width:32%!important")),
                () -> assertTrue(css.contains("flex:1 1 auto!important")),
                () -> assertTrue(css.contains("@media(max-width:760px)")),
                () -> assertTrue(AtfReportTheme.css().contains(css),
                        "Fails layout CSS must be composed into the final theme"));
    }
}
