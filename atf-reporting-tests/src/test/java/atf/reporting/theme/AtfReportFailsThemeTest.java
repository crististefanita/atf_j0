package atf.reporting.theme;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportFailsThemeTest {
    @Test
    void failsViewUsesTheSharedDarkSurfaceAndOwnsTheVendorStatusConflict() {
        String css = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(css.contains(".atf-fails-view{padding:1.5rem!important;overflow:auto!important;background:var(--atf-dark)!important")),
                () -> assertTrue(css.contains(".atf-failure>.status{order:2;flex:0 0 auto;display:inline-flex!important")),
                () -> assertTrue(css.contains("background:var(--atf-fail)!important;color:var(--atf-fail-text)!important")),
                () -> assertTrue(css.contains("#atf-fails-view.exception-view.atf-fails-view{display:none!important}")),
                () -> assertTrue(css.contains("#atf-fails-view.exception-view.atf-fails-view:not(.hide){display:flex!important")),
                () -> assertTrue(css.contains("flex-direction:row!important;flex-wrap:nowrap!important"),
                        "Fails list and detail must stay on the same horizontal row"),
                () -> assertTrue(css.contains("width:100%!important;box-sizing:border-box!important"),
                        "Fails projection must own the full report width"),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-list{flex:0 0 32%!important")),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-content{display:block!important;position:relative!important;left:auto!important;right:auto!important;top:auto!important;bottom:auto!important;width:auto!important;max-width:none!important;height:auto!important;flex:1 1 auto!important"),
                        "Fails detail must remain in the flex row instead of inheriting the vendor fixed off-canvas position"),
                () -> assertTrue(css.contains("@media(max-width:760px){#atf-fails-view.exception-view.atf-fails-view:not(.hide){"
                                + "left:var(--atf-narrow-nav-inset)!important;right:auto!important;"
                                + "width:calc(100% - var(--atf-narrow-nav-inset))!important;"
                                + "max-width:calc(100% - var(--atf-narrow-nav-inset))!important;box-sizing:border-box!important;"),
                        "Fails view must begin after the visible navigation rail on narrow reports"),
                () -> assertTrue(css.contains(".atf-failure-index{margin:28px!important")),
                () -> assertTrue(css.contains(".atf-failure-index-row")),
                () -> assertTrue(css.contains(".atf-failure-open")),
                () -> assertTrue(css.contains("gap:12px!important;width:100%!important;min-width:0!important;min-height:0!important")),
                () -> assertTrue(css.contains("font-size:10.08px!important;line-height:12.6px!important")),
                () -> assertTrue(css.contains("[status=\"fail\"]{border-left-color:var(--atf-bad)!important")),
                () -> assertTrue(css.contains(".test-item .test-detail .name{font-family:var(--atf-font-sans)!important;font-weight:400!important;color:var(--atf-card-text)!important")),
                () -> assertTrue(!css.contains("color:#ddd"), "Fails must consume the shared card-text token"),
                () -> assertTrue(css.contains(".test-item .test-detail .text-sm{font-family:var(--atf-font-sans)!important}")),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-status .badge{display:inline-flex!important;align-items:center!important;justify-content:center!important;min-width:0!important;width:auto!important;height:var(--atf-badge-height)!important")
                        && css.contains("padding:var(--atf-badge-padding)!important;border:0!important;border-radius:var(--atf-badge-radius)!important")
                        && css.contains("font-size:var(--atf-badge-font-size)!important;line-height:var(--atf-badge-line-height)!important;font-weight:400!important"),
                        "Fails status labels must use the compact shared geometry"),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-status .badge.badge-danger{background:var(--atf-fail)!important;color:var(--atf-fail-text)!important}"),
                        "Fails must use the shared red Fail status colors instead of the vendor accent"));
    }
}
