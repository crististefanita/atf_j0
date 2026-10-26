package atf.reporting.markup;
import atf.testkit.AtfUnitTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@AtfUnitTest
class ExtentReportMarkupContractTest {
    @Test
    void navigationMarkupPublishesStableViewIdsAndDeepLinks() {
        String source = "<li class=\"nav-item dropdown\" onclick=\"toggleView('dashboard-view')\">"
                + "<a class=\"dropdown-toggle\" href=\"#\"></a></li>"
                + "<a id=\"nav-test\" class=\"dropdown-toggle\" href=\"#\"></a>"
                + "<a id=\"nav-category\" class=\"dropdown-toggle\" href=\"#\"></a>"
                + "<div class=\"container-fluid p-4 view dashboard-view\"></div>";

        String result = ExtentReportNavigationView.inject(source);

        assertAll(
                () -> assertTrue(result.contains("id=\"nav-overview\"")),
                () -> assertTrue(result.contains("id=\"nav-fails\"")),
                () -> assertTrue(result.contains("href=\"#test-view\"")),
                () -> assertTrue(result.contains("href=\"#category-view\"")),
                () -> assertTrue(result.contains("href=\"#atf-fails-view\"")),
                () -> assertFalse(result.contains("onclick=\""),
                        "Navigation markup must delegate activation to the native hash controller"),
                () -> assertTrue(result.contains("id=\"atf-fails-view\"")),
                () -> assertTrue(result.contains("id=\"atf-failure-list\"")),
                () -> assertTrue(result.contains("id=\"dashboard-view\"")));
    }

    @Test
    void overviewMarkupKeepsAuditSecondaryAndCollapsedByDefault() {
        String marker = "<div class=\"row\">\n<div class=\"col-lg-6 col-md-12 category-container\">";

        String result = ExtentReportOverviewMarkup.inject("prefix" + marker + "suffix");

        assertAll(
                () -> assertTrue(result.contains("id=\"atf-execution-metadata\"")),
                () -> assertTrue(result.contains("id=\"atf-tag-audit\"")),
                () -> assertTrue(result.contains("collapsed by default")),
                () -> assertTrue(result.contains("id=\"atf-audit-scenarios\"")),
                () -> assertTrue(result.contains("id=\"atf-audit-packages\"")),
                () -> assertTrue(result.contains("id=\"atf-audit-tags\"")),
                () -> assertTrue(result.contains("category-container")),
                () -> assertTrue(result.endsWith("suffix")));
    }

    @Test
    void overviewInjectionUsesSemanticAnchorAndIsIdempotent() {
        String vendorVariant = "prefix<div class=\"row\">\n  <div class=\"col-md-12 col-lg-6 category-container\">suffix";

        String result = ExtentReportOverviewMarkup.inject(vendorVariant);
        String reinjected = ExtentReportOverviewMarkup.inject(result);

        assertAll(
                () -> assertTrue(result.contains("id=\"atf-execution-metadata\"")),
                () -> assertTrue(result.indexOf("id=\"atf-execution-metadata\"")
                        < result.indexOf("category-container")),
                () -> assertTrue(result.contains("collapsed by default")),
                () -> assertTrue(reinjected.equals(result),
                        "overview metadata must not be duplicated on a second publication pass"));
    }
}
