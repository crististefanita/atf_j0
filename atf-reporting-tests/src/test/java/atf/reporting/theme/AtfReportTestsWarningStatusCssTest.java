package atf.reporting.theme;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportTestsWarningStatusCssTest {
    @Test
    void testsWarningBadgeUsesTheAgreedOrangeAndWhiteText() {
        String css = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(css.contains(".test-view .test-list .badge.warning-bg,.test-view .test-list .badge.badge-warning,.test-view .test-contents .badge.warning-bg,.test-view .test-contents .badge.badge-warning,.test-content .badge.warning-bg,.test-content .badge.badge-warning{background:var(--atf-skip)!important;color:#fff!important}")),
                () -> assertTrue(css.contains(".test-view .test-list .badge.warning-bg"),
                        "Tests navigation must style semantic warning badges"),
                () -> assertTrue(css.contains(".test-content .badge.badge-warning"),
                        "Tests detail must style the alternate warning badge class"));
    }
}
