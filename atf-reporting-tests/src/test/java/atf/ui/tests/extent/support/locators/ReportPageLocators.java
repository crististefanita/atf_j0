package atf.ui.tests.extent.support.locators;

import atf.reporting.theme.tokens.AtfReportCssSelectors;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Locators for report surfaces, tabs and the overview projection. */
final class ReportPageLocators {
    private static final String TESTS_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.TEST_VIEW);
    private static final String OVERVIEW_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.DASHBOARD_VIEW);
    private static final String CATEGORY_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.CATEGORY_VIEW);
    private static final String FAILS_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.FAILS_VIEW);
    private static final String TEST_LIST = AtfReportCssSelectors.Component.TEST_LIST.attributeSelector();
    private static final String TEST_LIST_ITEMS =
            AtfReportCssSelectors.Component.TEST_LIST_ITEM.attributeSelector();
    private static final String VISIBLE_TEST_DETAIL = ReportLocatorSupport.VISIBLE_TEST_DETAIL;
    private static final String EXECUTION_METADATA = ReportLocatorSupport.idSelector("atf-execution-metadata");
    private static final String SECONDARY_OVERVIEW_ZONES =
            "details" + ReportLocatorSupport.idSelector("atf-execution-metadata") + ",details"
                    + ReportLocatorSupport.idSelector("atf-tag-audit");
    private static final String OPEN_DISCLOSURES = "details[open], [class~='collapse'][class~='show']";
    private static final String ALL_DISCLOSURES = "details";
    private static final String COLLAPSED_REPORT_PANELS = "[class~='collapse']:not([class~='show'])";
    private static final String DECORATIVE_SEPARATORS = "hr, [class~='atf-ui-zone-separator']";
    private static final String IMAGES = "img";
    private static final String SECONDARY_BODY = "[class~='atf-secondary-body']";
    private static final String TAG_AUDIT_BODY = "[class~='atf-tag-audit-body']";
    private static final String VISIBLE_CLOSED_SECONDARY_BODIES =
            "details:not([open]) > " + SECONDARY_BODY + ":visible,details:not([open]) > "
                    + TAG_AUDIT_BODY + ":visible";

    Locator exactText(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    Locator exactLink(Locator scope, String text) {
        return scope.getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName(text).setExact(true));
    }

    Locator testsView(Page page) { return page.locator(TESTS_VIEW); }
    Locator runtimeRoot(Page page) { return page.locator("html"); }
    Locator decorativeSeparators(Page page) { return page.locator(DECORATIVE_SEPARATORS); }
    Locator images(Page page) { return page.locator(IMAGES); }
    Locator navTab(Page page, ReportUiContract.Tab tab) { return page.locator(navTabSelector(tab)); }

    AtfLocator navTabAction(Page page, ReportUiContract.Tab tab) {
        return new AtfLocator(navTab(page, tab), navTabSelector(tab));
    }

    Locator overviewView(Page page) { return page.locator(OVERVIEW_VIEW); }
    Locator categoryView(Page page) { return page.locator(CATEGORY_VIEW); }
    Locator failsView(Page page) { return page.locator(FAILS_VIEW); }

    Locator tabView(Page page, ReportUiContract.Tab tab) {
        return switch (tab) {
            case TESTS -> testsView(page);
            case OVERVIEW -> overviewView(page);
            case CATEGORIES -> categoryView(page);
            case FAILS -> failsView(page);
        };
    }

    Locator tabViewByHash(Page page, String hash) {
        for (ReportUiContract.Tab tab : ReportUiContract.Tab.values()) {
            if (tab.hash().equals(hash)) return tabView(page, tab);
        }
        throw new IllegalArgumentException("Unsupported report tab hash: " + hash);
    }

    ReportUiContract.Tab tabByHash(String hash) {
        for (ReportUiContract.Tab tab : ReportUiContract.Tab.values()) {
            if (tab.hash().equals(hash)) return tab;
        }
        throw new IllegalArgumentException("Unsupported report tab hash: " + hash);
    }

    Locator testsList(Page page) { return testsView(page).locator(TEST_LIST); }
    Locator visibleDisclosureHeaders(Page page, ReportUiContract.Tab tab) {
        return tabView(page, tab).locator(AtfReportCssSelectors.Component.CARD_HEADER.attributeSelector()
                + "[role='button']:visible");
    }
    Locator disclosureHeaderNodes(Locator header) {
        return header.locator(AtfReportCssSelectors.Component.CARD_TITLE.attributeSelector()
                + " " + AtfReportCssSelectors.Component.NODE.attributeSelector());
    }
    Locator openDisclosures(Locator scope) { return scope.locator(OPEN_DISCLOSURES); }
    Locator allDisclosures(Locator scope) { return scope.locator(ALL_DISCLOSURES); }
    Locator collapsedReportPanels(Locator scope) { return scope.locator(COLLAPSED_REPORT_PANELS); }
    Locator overviewCard(Page page) {
        return overviewView(page).locator(AtfReportCssSelectors.Component.CARD.attributeSelector()).first();
    }
    Locator categoryList(Page page) { return categoryView(page).locator(TEST_LIST); }
    Locator categoryList(Locator category) { return category.locator(TEST_LIST); }
    Locator testContentDetail(Page page) { return testsView(page).locator(VISIBLE_TEST_DETAIL).last(); }
    Locator categoryContentDetail(Page page) { return categoryView(page).locator(VISIBLE_TEST_DETAIL); }
    Locator overviewTables(Locator overview) { return overview.locator("table"); }
    Locator overviewSection(Locator overview, String selector) { return overview.locator(selector); }
    Locator overviewMetrics(Locator overview) {
        return overview.locator(ReportUiContract.OVERVIEW_EXECUTION_METADATA_GRID
                + " " + ReportUiContract.OVERVIEW_SECONDARY_METRIC);
    }
    Locator overviewTagAuditTables(Locator overview) {
        return overviewSecondaryZoneBody(overview, ReportUiContract.OverviewSection.TAG_AUDIT)
                .locator(ReportLocatorSupport.classSelector("table", "atf-tag-table"));
    }
    Locator tableHeaders(Locator table) { return table.locator("thead th"); }
    Locator tableRows(Locator table) { return table.locator("tbody tr"); }
    Locator overviewExecutionMetadata(Locator overview) { return overview.locator(EXECUTION_METADATA); }
    Locator overviewSecondaryZones(Locator overview) { return overview.locator(SECONDARY_OVERVIEW_ZONES); }
    Locator overviewSecondaryZone(Locator overview, ReportUiContract.OverviewSection zone) {
        return overview.locator("details" + zone.selector());
    }
    Locator overviewSecondaryZoneBody(Locator overview, ReportUiContract.OverviewSection zone) {
        return overviewSecondaryZone(overview, zone).locator(":scope > "
                + secondaryBodySelector(zone));
    }
    Locator overviewOpenSecondaryZones(Locator overview) {
        return overview.locator(SECONDARY_OVERVIEW_ZONES.replace("details", "details[open]"));
    }
    Locator overviewVisibleClosedSecondaryBodies(Locator overview) {
        return overview.locator(VISIBLE_CLOSED_SECONDARY_BODIES);
    }
    Locator overviewPackageRows(Locator overview) {
        return overview.locator(ReportUiContract.OverviewSection.PACKAGE_AUDIT.selector()
                + " tbody tr");
    }
    Locator testPackageNodes(Page page) {
        return testsView(page).locator(TEST_LIST_ITEMS + " > "
                + AtfReportCssSelectors.Component.TEST_ITEM.attributeSelector()
                + " " + AtfReportCssSelectors.Component.TEST_CONTENTS.attributeSelector()
                + " [class~='detail-head'] :is([class~='atf-test-package'][data-atf-test-package], "
                + "[class~='p-v-10'] > [class~='m-t-10'])");
    }
    AtfLocator overviewExecutionMetadataAction(Page page) {
        return new AtfLocator(overviewExecutionMetadata(overviewView(page)).locator("summary"),
                OVERVIEW_VIEW + " " + EXECUTION_METADATA + " > summary");
    }
    AtfLocator overviewSecondaryZoneAction(Page page, ReportUiContract.OverviewSection zone) {
        return new AtfLocator(overviewSecondaryZone(overviewView(page), zone).locator("summary"),
                OVERVIEW_VIEW + " details" + zone.selector() + " > summary");
    }
    Locator overviewAuditRows(Locator overview) {
        return overview.locator(ReportUiContract.OVERVIEW_SCENARIO_AUDIT + " tbody tr");
    }

    private static String secondaryBodySelector(ReportUiContract.OverviewSection zone) {
        return switch (zone) {
            case EXECUTION_METADATA -> SECONDARY_BODY;
            case TAG_AUDIT -> TAG_AUDIT_BODY;
            default -> throw new IllegalArgumentException("Unsupported secondary Overview zone: " + zone);
        };
    }
    Locator metricLabel(Locator metric) { return metric.locator("small"); }
    Locator metricValue(Locator metric) { return metric.locator("strong"); }
    Locator tableCells(Locator row) { return row.locator("td"); }
    Locator overviewCategoryCells(Locator overview) {
        return overview.locator("[class~='category-container'] table tbody td");
    }

    private static String navTabSelector(ReportUiContract.Tab tab) {
        return "[id='nav-" + switch (tab) {
            case TESTS -> "test";
            case CATEGORIES -> "category";
            case FAILS -> "fails";
            case OVERVIEW -> "overview";
        } + "']";
    }
}

