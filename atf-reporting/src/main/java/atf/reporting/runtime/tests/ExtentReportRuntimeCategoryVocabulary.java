package atf.reporting.runtime.tests;

import atf.reporting.runtime.core.ExtentReportRuntimeVocabulary;

import java.util.Objects;

/** Typed browser vocabulary owned by the Categories runtime controller. */
final class ExtentReportRuntimeCategoryVocabulary {
    private ExtentReportRuntimeCategoryVocabulary() {
    }

    enum Function {
        CATEGORY_ITEMS("categoryItems"),
        CATEGORY_STATUS_VALUES("categoryStatusValues"),
        CATEGORY_HAS_STATUS("categoryHasStatus"),
        CATEGORY_STATUS("categoryStatus"),
        CATEGORY_NAME("categoryName"),
        CATEGORY_FILTER_MARKUP("categoryFilterMarkup"),
        REFRESH_CATEGORY_FILTER_OPTIONS("refreshCategoryFilterOptions"),
        REFRESH_CATEGORY_FILTER_SELECTION("refreshCategoryFilterSelection"),
        APPLY_CATEGORY_FILTERS("applyCategoryFilters"),
        SET_CATEGORY_FILTER("setCategoryFilter"),
        BIND_CATEGORY_FILTERS("bindCategoryFilters"),
        ENSURE_CATEGORY_FILTERS("ensureCategoryFilters"),
        CATEGORY_SCENARIO_INDEX("categoryScenarioIndex"),
        OPEN_CATEGORY_TEST("openCategoryTest"),
        NORMALIZE_CATEGORY_LINKS("normalizeCategoryLinks"),
        RENDER_CATEGORY_DETAIL("renderCategoryDetail"),
        SELECT_CATEGORY("selectCategory"),
        BIND_CATEGORY_SELECTION("bindCategorySelection"),
        ENSURE_CATEGORY_DETAIL("ensureCategoryDetail");

        private final String value;

        Function(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Selector {
        CATEGORY_ITEMS("#category-view .test-list-wrapper>.test-list-item>.test-item"),
        CATEGORY_SUMMARY_STATUSES(".test-contents .info [status]"),
        CATEGORY_ROW_STATUSES(".test-contents tr.tag-test-status[status]"),
        CATEGORY_NAME(".test-detail .name"),
        CATEGORY_STATUS_MENU("#category-status-toggle"),
        CATEGORY_TAG_MENU("#category-tag-toggle"),
        CATEGORY_TOOLS("#category-view>.test-list>.test-list-tools"),
        CATEGORY_TOOLS_RIGHT(":scope>.tools.text-right"),
        TEST_ITEM("#test-view .test-item[test-id=\"%s\"]"),
        SCENARIO_TITLE(".card-title .node"),
        CATEGORY_LINKS("#category-view .test-content-detail a.linked[test-id]"),
        CATEGORY_DETAIL("#category-view .test-content-detail .detail-body"),
        CATEGORY_SOURCE(":scope>.test-contents"),
        CATEGORY_TITLE(":scope>.info>h4");

        private final String value;

        Selector(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Attribute {
        STATUS("status"),
        FILTER_KIND("data-filter-kind"),
        FILTER_VALUE("data-filter-value"),
        FILTER_BOUND("data-atf-filter-bound"),
        FILTER_HIDDEN("data-atf-filter-hidden"),
        CATEGORY_BOUND("data-atf-category-bound"),
        ARIA_PRESSED("aria-pressed"),
        TEST_ID("test-id"),
        ATF_TEST_ID("data-atf-test-id"),
        SCENARIO_INDEX("data-atf-scenario-index"),
        TITLE("title"),
        CATEGORY_TITLE("data-atf-category-title");

        private final String value;

        Attribute(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Status {
        PASS("pass"),
        FAIL("fail"),
        WARNING("warning"),
        SKIP("skip"),
        CLEAR("clear");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    static String js(Selector selector) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(selector, "selector").value());
    }

    static String js(Attribute attribute) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(attribute, "attribute").value());
    }

    static String js(Status status) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(status, "status").value());
    }

    static String function(Function function) {
        return Objects.requireNonNull(function, "function").value();
    }

    static String dynamicSelector(Selector selector, String expression) {
        String template = Objects.requireNonNull(selector, "selector").value();
        String dynamicExpression = Objects.requireNonNull(expression, "expression");
        int placeholder = template.indexOf("%s");
        if (placeholder < 0 || placeholder != template.lastIndexOf("%s")) {
            throw new IllegalArgumentException("Selector must contain exactly one %s placeholder: " + template);
        }
        return ExtentReportRuntimeVocabulary.js(template.substring(0, placeholder))
                + "+" + dynamicExpression + "+"
                + ExtentReportRuntimeVocabulary.js(template.substring(placeholder + 2));
    }
}
