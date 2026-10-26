package atf.reporting.theme;
import atf.testkit.AtfUnitTest;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportFailsStatusCssTest {
    @Test
    void failsProjectionReusesTheSharedStatusGeometry() {
        String fragment = AtfReportFailsStatusCss.css();
        String theme = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(!fragment.isBlank()),
                () -> assertTrue(fragment.contains("height:var(--atf-badge-height)!important")),
                () -> assertTrue(fragment.contains("padding:var(--atf-badge-padding)!important")),
                () -> assertTrue(fragment.contains("font-weight:400!important")),
                () -> assertTrue(fragment.contains(".exception-view.atf-fails-view .test-status .badge.warning-bg,.exception-view.atf-fails-view .test-status .badge.badge-warning{background:var(--atf-skip)!important;color:#fff!important}")),
                () -> assertTrue(fragment.contains("color:#fff!important}"),
                        "Fails warning badges must use white text"),
                () -> assertTrue(fragment.contains("background:var(--atf-fail)!important")),
                () -> assertTrue(theme.contains(fragment),
                        "Fails status fragment must be composed into the final theme"),
                () -> assertTrue(theme.contains(".atf-failure>.status{order:2;flex:0 0 auto;display:inline-flex!important")
                                && theme.contains(".atf-failure>.status{order:2;flex:0 0 auto;display:inline-flex!important;align-items:center!important;justify-content:center!important;min-width:0!important;height:var(--atf-badge-height)!important"),
                        "failure detail status must not retain a local fixed geometry"));
    }
}
