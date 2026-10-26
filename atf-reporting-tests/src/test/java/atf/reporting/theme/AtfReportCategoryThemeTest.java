package atf.reporting.theme;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

    /** Protects the semantic palette and shared badge geometry in Category. */
@AtfUnitTest
class AtfReportCategoryThemeTest {
    @Test
    void categoryStatusBadgesUseTheReferenceSemanticPalette() {
        String css = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(css.contains(".category-view .test-list .test-detail .meta .badge.log,.category-view .test-contents .info .badge.log")),
                () -> assertTrue(css.contains(".category-view .test-contents td .badge.log")),
                () -> assertTrue(css.contains("height:var(--atf-badge-height)!important")),
                () -> assertTrue(css.contains("padding:var(--atf-badge-padding)!important")),
                () -> assertTrue(css.contains("border:0!important;border-radius:var(--atf-badge-radius)!important")),
                () -> assertTrue(css.contains("font-size:var(--atf-badge-font-size)!important;line-height:var(--atf-badge-line-height)!important")),
                () -> assertTrue(css.contains("min-width:0!important;width:auto!important")),
                () -> assertTrue(!css.contains("--atf-category-badge-"), "Category must reuse the shared badge geometry"),
                () -> assertTrue(css.contains("badge.log.pass-bg,.category-view .test-contents .info .badge.log.pass-bg,.category-view .test-contents td .badge.log.pass-bg{background:var(--atf-pass)!important;color:var(--atf-pass-text)!important}")),
                () -> assertTrue(css.contains("badge.log.badge-danger,.category-view .test-contents .info .badge.log.badge-danger,.category-view .test-contents td .badge.log.badge-danger{background:var(--atf-fail)!important;color:var(--atf-fail-text)!important}")),
                () -> assertTrue(css.contains(".category-view .test-list .test-detail .meta .badge.log.fail-bg,.category-view .test-contents .info .badge.log.fail-bg,.category-view .test-contents td .badge.log.fail-bg{background:var(--atf-fail)!important;color:var(--atf-fail-text)!important}"),
                        "Category must normalize the concrete Extent fail class as well as the semantic danger class"),
                () -> assertTrue(css.contains("badge.log.badge-skip,.category-view .test-contents .info .badge.log.badge-skip,.category-view .test-contents td .badge.log.badge-skip{background:var(--atf-skip)!important;color:var(--atf-skip-text)!important}")),
                () -> assertTrue(css.contains("badge.log.badge-warning,.category-view .test-contents .info .badge.log.badge-warning,.category-view .test-contents td .badge.log.badge-warning{background:var(--atf-skip)!important;color:#fff!important}")),
                () -> assertTrue(css.contains(".category-view .test-list .test-detail .meta .badge.log.warning-bg,.category-view .test-contents .info .badge.log.warning-bg,.category-view .test-contents td .badge.log.warning-bg{background:var(--atf-skip)!important;color:#fff!important}"),
                        "Category must normalize the concrete Extent warning class so summary and detail use one warning palette"));
    }

    @Test
    void categoryViewUsesOneSurfaceAndAccentLinks() {
        String css = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(css.contains(".category-view,.category-view .test-list,.category-view .test-list-tools,")),
                () -> assertTrue(css.contains(".category-view .test-content,.category-view .test-content-tools,")),
                () -> assertTrue(css.contains(".category-view .test-list-wrapper>.test-list-item>.test-item")),
                () -> assertTrue(css.contains(".category-view a.linked{color:var(--atf-accent)!important")),
                () -> assertTrue(css.contains(".category-view a.linked:hover,.category-view a.linked:focus")),
                () -> assertTrue(css.contains(".category-view .test-list-tools .dropdown-menu")),
                () -> assertTrue(css.contains(".category-view .test-list-tools .dropdown-menu .dropdown-item")));
    }
}
