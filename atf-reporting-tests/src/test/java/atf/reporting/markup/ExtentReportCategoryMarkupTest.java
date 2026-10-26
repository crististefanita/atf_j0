package atf.reporting.markup;
import atf.testkit.AtfUnitTest;
import atf.reporting.theme.AtfReportTheme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportCategoryMarkupTest {
    @Test
    void preservesExistingVendorCategoryMarkup() {
        String existing = "<main><div id=\"category-view\" class=\"vendor\"></div></main>";

        assertEquals(existing, ExtentReportCategoryMarkup.ensureView(existing));
    }

    @Test
    void publishesStableEmptyCategoryShellWhenVendorOmitsCategories() {
        String report = "<main><div id=\"atf-fails-view\" class=\"test-wrapper row view exception-view atf-fails-view\"></div></main>";
        String result = ExtentReportCategoryMarkup.ensureView(report);

        assertAll(
                () -> assertTrue(result.contains("id=\"category-view\"")),
                () -> assertTrue(result.contains("class=\"test-list-item\"></ul>")),
                () -> assertTrue(result.contains("data-atf-empty-state=\"categories\"")),
                () -> assertTrue(result.contains("No categories available")),
                () -> assertTrue(result.indexOf("id=\"category-view\"")
                        < result.indexOf("class=\"test-wrapper row view exception-view")));
    }

    @Test
    void publishesTheCompleteTypedCategoryContract() {
        String result = ExtentReportCategoryMarkup.emptyView();

        assertAll(
                () -> assertTrue(result.contains("class=\"test-list-tools\"")),
                () -> assertTrue(result.contains("class=\"tools pull-left\"")),
                () -> assertTrue(result.contains("class=\"tools text-right\"")),
                () -> assertTrue(result.contains("class=\"test-list-wrapper scrollable\"")),
                () -> assertTrue(result.contains("class=\"test-content scrollable\"")),
                () -> assertTrue(result.contains("class=\"back-to-test\" href=\"#\"")),
                () -> assertTrue(result.contains("role=\"status\"")),
                () -> assertTrue(result.contains("class=\"detail-body atf-empty-state\"")));
    }

    @Test
    void keepsCategoryEmptyStateOnTheSharedVisualContract() {
        String css = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(css.contains(".category-view .atf-empty-state")),
                () -> assertTrue(css.contains(".category-view .atf-empty-state-title")),
                () -> assertTrue(css.contains("border-left:3px solid var(--atf-accent)!important")));
    }
}
