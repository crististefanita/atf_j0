package atf.reporting.runtime.tests;

import atf.reporting.runtime.core.ExtentReportRuntimeVocabulary;

import java.util.Arrays;
import java.util.Objects;

/** Typed DOM and JavaScript vocabulary shared by the Tests runtime controllers. */
final class ExtentReportRuntimeTestsVocabulary {
    private ExtentReportRuntimeTestsVocabulary() {
    }

    enum Function {
        FILTER_MATCHES("filterMatches"),
        REFRESH_FILTER_SELECTION("refreshFilterSelection"),
        APPLY_TEST_FILTERS("applyTestFilters"),
        SET_TEST_FILTER("setTestFilter"),
        BIND_TEST_FILTERS("bindTestFilters"),
        NORMALIZE_TAG_FILTER("normalizeTagFilter"),
        IS_SCENARIO_CARD("isScenarioCard"),
        NORMALIZE_SCENARIO_ANNOTATIONS("normalizeScenarioAnnotations");

        private final String value;

        Function(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Template {
        FILTER_STATE_STATUS("FILTER_STATE_STATUS"),
        FILTER_STATE_TAG("FILTER_STATE_TAG"),
        FILTER_MATCHES("FILTER_MATCHES"),
        REFRESH_FILTER_SELECTION("REFRESH_FILTER_SELECTION"),
        APPLY_TEST_FILTERS("APPLY_TEST_FILTERS"),
        SET_TEST_FILTER("SET_TEST_FILTER"),
        BIND_TEST_FILTERS("BIND_TEST_FILTERS"),
        NORMALIZE_TAG_FILTER("NORMALIZE_TAG_FILTER"),
        FILTER_ITEMS("FILTER_ITEMS"),
        STATUS_MENU("STATUS_MENU"),
        TAG_FILTER_ITEMS("TAG_FILTER_ITEMS"),
        ACTIVE_TEST("ACTIVE_TEST"),
        TEST_DETAIL("TEST_DETAIL"),
        CLASS_ITEMS("CLASS_ITEMS"),
        IS_CLASS_ROOT("IS_CLASS_ROOT"),
        DIRECT_SCENARIO_CARDS("DIRECT_SCENARIO_CARDS"),
        STATUS_OF("STATUS_OF"),
        TEST_TAGS("TEST_TAGS"),
        QUERY_ALL("QUERY_ALL"),
        TEXT("TEXT"),
        AGGREGATE_CLASS_DURATIONS("AGGREGATE_CLASS_DURATIONS"),
        CAPTURE_DOM_CONTRACT("CAPTURE_DOM_CONTRACT"),
        STATUS_ATTRIBUTE("STATUS_ATTRIBUTE"),
        FILTER_BOUND_ATTRIBUTE("FILTER_BOUND_ATTRIBUTE"),
        ARIA_PRESSED_ATTRIBUTE("ARIA_PRESSED_ATTRIBUTE"),
        FILTER_HIDDEN_ATTRIBUTE("FILTER_HIDDEN_ATTRIBUTE"),
        TRUE_VALUE("TRUE_VALUE"),
        ACTIVE_CLASS("ACTIVE_CLASS"),
        CLICK_EVENT("CLICK_EVENT"),
        CLEAR_VALUE("CLEAR_VALUE"),
        DISPLAY_PROPERTY("DISPLAY_PROPERTY"),
        EMPTY_VALUE("EMPTY_VALUE"),
        NONE_VALUE("NONE_VALUE"),
        IMPORTANT_PRIORITY("IMPORTANT_PRIORITY"),
        EXCLUDED_TAGS("EXCLUDED_TAGS");

        private final String value;

        Template(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Selector {
        FILTER_ITEMS("#status-toggle .dropdown-item,#tag-toggle .dropdown-item"),
        STATUS_MENU("#status-toggle"),
        TAG_FILTER_ITEMS("#tag-toggle .dropdown-item"),
        ACTIVE_TEST(".test-list-item>.test-item.active"),
        TEST_DETAIL(".test-detail"),
        SCENARIO_CARDS(".test-contents > .mt-4 > .accordion > .card[data-atf-scenario-id]"),
        SCENARIO_HEADER(":scope > .card-header"),
        SCENARIO_TITLE(":scope > .card-title"),
        SCENARIO_ANNOTATIONS(":scope > .atf-scenario-annotations"),
        SCENARIO_SOURCE(":scope > .accordion > .card .card-title > .node-attr"),
        DIRECT_NODE_ATTRIBUTES(":scope > .node-attr"),
        NESTED_NODE_ATTRIBUTES(":scope > .accordion .node-attr"),
        BADGES(".badge");

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
        FILTER_BOUND("data-atf-filter-bound"),
        ARIA_PRESSED("aria-pressed"),
        FILTER_HIDDEN("data-atf-filter-hidden"),
        SCENARIO_TAGS("data-atf-scenario-tags"),
        ROLE("role"),
        ARIA_LABEL("aria-label"),
        TAG_KIND("data-atf-tag-kind");

        private final String value;

        Attribute(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum CssClass {
        ACTIVE("active"),
        ACCORDION("accordion"),
        SCENARIO_HOST("mt-4"),
        TEST_CONTENTS("test-contents"),
        SCENARIO_ANNOTATIONS("atf-scenario-annotations"),
        TAG_ANNOTATION("atf-tag-annotation"),
        TAG_ANNOTATION_SELECTION("atf-tag-annotation-selection");

        private final String value;

        CssClass(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Event {
        CLICK("click");

        private final String value;

        Event(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum Value {
        CLEAR("clear"),
        LIST("list"),
        LIST_ITEM("listitem"),
        STATUS_KEY("status"),
        TAG_KEY("tag"),
        TRUE("true"),
        SCENARIO_ANNOTATIONS_LABEL("Scenario annotations"),
        TAG_KIND_SELECTION("selection");

        private final String value;

        Value(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum StyleProperty {
        DISPLAY("display");

        private final String value;

        StyleProperty(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum StyleValue {
        EMPTY(""),
        NONE("none");

        private final String value;

        StyleValue(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum StylePriority {
        IMPORTANT("important");

        private final String value;

        StylePriority(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    enum ExcludedTag {
        FAILURE("failure"),
        REPORTING("reporting"),
        SKIP("skip"),
        SOFT_ASSERT("soft-assert"),
        TEARDOWN("teardown"),
        WARNING("warning");

        private final String value;

        ExcludedTag(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    static String function(Function function, String parameters, String body) {
        return "function " + function(function) + "(" + parameters + "){" + body + "}";
    }

    static String function(Function function) {
        return Objects.requireNonNull(function, "function").value();
    }

    static String js(Selector selector) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(selector, "selector").value());
    }

    static String js(Attribute attribute) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(attribute, "attribute").value());
    }

    static String js(CssClass cssClass) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(cssClass, "cssClass").value());
    }

    static String js(Event event) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(event, "event").value());
    }

    static String js(Value value) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(value, "value").value());
    }

    static String js(StyleProperty property) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(property, "property").value());
    }

    static String js(StyleValue value) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(value, "value").value());
    }

    static String js(StylePriority priority) {
        return ExtentReportRuntimeVocabulary.js(Objects.requireNonNull(priority, "priority").value());
    }

    static String js(String value) {
        return ExtentReportRuntimeVocabulary.js(value);
    }

    static String js(CssClass first, CssClass second) {
        return js(css(first, second));
    }

    static String css(CssClass... classes) {
        return Arrays.stream(classes)
                .filter(Objects::nonNull)
                .map(CssClass::value)
                .reduce((first, second) -> first + " " + second)
                .orElse("");
    }

    static String excludedTags() {
        return Arrays.stream(ExcludedTag.values())
                .map(tag -> js(tag.value()) + ":true")
                .reduce((first, second) -> first + "," + second)
                .orElse("");
    }
}
