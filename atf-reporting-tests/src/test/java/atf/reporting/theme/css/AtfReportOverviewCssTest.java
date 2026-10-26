package atf.reporting.theme.css;
import atf.testkit.AtfUnitTest;
import atf.reporting.theme.AtfReportTheme;
import atf.reporting.asset.ReportAssetComposer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportOverviewCssTest {
    @Test
    void overviewProvidersOwnIndependentCssResponsibilities() {
        String layout = AtfReportOverviewCss.layout();
        String tagAudit = AtfReportOverviewCss.tagAudit();
        String categoryTables = AtfReportOverviewCss.categoryTables();
        String overview = AtfReportOverviewCss.metrics();
        String categoryStatus = AtfReportOverviewCss.categoryStatus();

        assertAll(
                () -> assertTrue(layout.contains(".atf-secondary-overview>summary")),
                () -> assertTrue(layout.contains(".dashboard-view>.row>.atf-secondary-overview")),
                () -> assertTrue(layout.contains(".atf-secondary-overview:not([open])>:not(summary)")),
                () -> assertTrue(layout.contains(".atf-secondary-overview[open]>:not(summary)")),
                () -> assertTrue(tagAudit.contains(".atf-tag-table{width:100%!important")),
                () -> assertTrue(categoryTables.contains(".dashboard-view .category-container>.card>.card-body")),
                () -> assertTrue(ReportAssetComposer.tableViewportCss()
                                .contains("height:auto!important;min-height:0!important;max-height:var(--atf-table-scroll-height)!important"),
                        "short Overview/System tables must stay content-sized under the shared scroll cap"),
                () -> assertTrue(categoryTables.contains("position:sticky!important;top:0!important")),
                () -> assertTrue(categoryStatus.contains(".category-view .test-list")),
                () -> assertEquals(layout + tagAudit + categoryTables, overview),
                () -> assertFalse(overview.contains(".category-view .test-list")),
                () -> assertFalse(overview.contains("${")),
                () -> assertTrue(overview.contains("var(--atf-overview-font-family)")),
                () -> assertTrue(overview.contains("var(--atf-overview-font-size)")));
    }

    @Test
    void themeComposesOverviewAfterExecutionRules() {
        String theme = AtfReportTheme.css();
        String overview = AtfReportOverviewCss.metrics();

        int overviewStart = theme.indexOf(overview);
        int executionAnchor = theme.indexOf(".test-view .test-content-detail");

        assertAll(
                () -> assertTrue(overviewStart >= 0, "overview provider must be present in the final theme"),
                () -> assertTrue(executionAnchor >= 0),
                () -> assertTrue(executionAnchor < overviewStart,
                        "overview precedence must remain after execution-page rules"),
                () -> assertTrue(theme.contains(AtfReportOverviewCss.categoryStatus())));
    }

    @Test
    void narrowOverviewUsesTheSpaceRemainingAfterTheFixedNavigationRail() {
        String layout = AtfReportOverviewCss.layout();

        assertAll(
                () -> assertTrue(layout.contains(".dashboard-view{padding:21px!important}")),
                () -> assertTrue(layout.contains("@media (max-width:991px)")),
                () -> assertTrue(layout.contains("margin-left:70px!important")),
                () -> assertTrue(layout.contains("width:auto!important")));
    }
}
