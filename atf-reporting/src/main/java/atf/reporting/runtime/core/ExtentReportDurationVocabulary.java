package atf.reporting.runtime.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Typed browser contract for the duration runtime.
 *
 * <p>Duration code emits JavaScript, but it operates on a stable HTML/CSS
 * vocabulary. Keeping attributes, selectors and function names together makes
 * the runtime fragments readable and gives component tests one seam for
 * detecting contract drift.</p>
 */
public final class ExtentReportDurationVocabulary {
    private ExtentReportDurationVocabulary() {
    }

    public enum Attribute {
        DURATION_MS("data-duration-ms"),
        DURATION_SOURCE("data-duration-source"),
        MEASURED_DURATION_MS("data-atf-measured-duration-ms"),
        FILTER_HIDDEN("data-atf-filter-hidden"),
        ORIGINAL_LABEL("data-atf-original-label"),
        DURATION_READY("data-atf-duration-ready"),
        DURATION_CLICK_BOUND("data-atf-duration-click-bound"),
        DURATION_WATCH("data-atf-duration-watch"),
        TEST_ID("test-id"),
        ATF_TEST_ID("data-atf-test-id"),
        SCENARIO_INDEX("data-atf-scenario-index"),
        ATF_NODE_ROLE("data-atf-node-role"),
        SCENARIO_ID("data-atf-scenario-id");

        private final String value;

        Attribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** CSS selectors are kept semantic so fragments do not repeat vendor markup details. */
    public enum Selector {
        TEST_VIEW("#test-view"),
        CATEGORY_VIEW("#category-view"),
        DASHBOARD_VIEW("#dashboard-view"),
        FAILS_VIEW("#atf-fails-view"),
        TIMELINE("#timeline"),
        TEST_LIST_ITEM(".test-list-item"),
        TEST_ITEM(".test-item"),
        TEST_CONTENTS(".test-contents"),
        TEST_CONTENT_DETAIL(".test-content-detail"),
        TEST_DETAIL_TEXT(".test-detail .text-sm"),
        TEST_DETAIL_DURATION(".test-detail .duration"),
        DETAIL_HEAD_INFO(".detail-head .info"),
        CARD(".card"),
        CARD_HEADER(".card-header"),
        CARD_TITLE_NODE(".card-title a.node"),
        CARD_TOOLBAR_BADGE(".card-toolbar .badge"),
        CARD_TOOLBAR_BADGE_DEFAULT(".card-toolbar .badge-default"),
        NODE_INFO_BADGE(".node-info .badge-default"),
        ACCORDION(".accordion"),
        UI_ACTION_ROW(".atf-ui-action-table tbody tr"),
        UI_ACTION_TIME(".atf-ui-action-time"),
        COLLAPSE(".collapse"),
        EXECUTION_TABLE(".execution-table"),
        HELM_TRACE_TABLE(".helm-trace-table"),
        HELM_REQUEST_DETAILS_CONTENT(".helm-request-details-content"),
        HELM_RESPONSE_DETAILS_CONTENT(".helm-response-details-content"),
        CATEGORY_ITEM(".test-list-item>.test-item"),
        CATEGORY_LINK("a.linked[test-id],a.linked[data-atf-test-id]"),
        CATEGORY_DURATION(".test-detail .duration"),
        CATEGORY_TEXT(".test-detail .text-sm"),
        CLASS_ITEMS(".test-list-item > .test-item[test-id]"),
        DIRECT_SCENARIO_CARDS(".test-contents > .mt-4 > .accordion > .card"),
        PHASE_CARD(".card[data-atf-node-role='phase']"),
        TEST_NAVIGATOR_DURATION(".test-view .test-list-item .test-detail .text-sm"),
        TEST_DETAIL_HEADER_BADGE(".test-view .test-content-detail .card-toolbar .badge"),
        CATEGORY_HEADER_BADGE(".category-view .test-content-detail .card-toolbar .badge"),
        FAIL_ROW(".atf-failure"),
        DETAIL_HEAD_BADGE(".detail-head .info > .badge-default:not(.uri-anchor):not(.pointer)");

        private final String value;

        Selector(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** JavaScript function names are constants, not implicit string literals. */
    public enum Function {
        ATF_DURATION_VALUE("atfDurationValue"),
        ATF_FORMAT_DURATION("atfFormatDuration"),
        ATF_NORMALIZE_DURATION("atfNormalizeDuration"),
        NORMALIZE_TEST_NAVIGATOR_SEPARATORS("normalizeTestNavigatorSeparators"),
        NORMALIZE_HEADER_METADATA("normalizeHeaderMetadata"),
        NORMALIZE_DURATIONS("normalizeDurations"),
        DIRECT_CARD_HEADER("directCardHeader"),
        DIRECT_NODE("directNode"),
        DURATION_BADGE("durationBadge"),
        NODE_LABEL("nodeLabel"),
        NODE_PATH_KEY("nodePathKey"),
        NODE_IDENTITY("nodeIdentity"),
        REMEMBER_MEASURED_DURATIONS("rememberMeasuredDurations"),
        MEASURED_NODE_DURATION("measuredNodeDuration"),
        DURATION_CHILD_VISIBLE("durationChildVisible"),
        IS_STRUCTURAL_NODE("isStructuralNode"),
        DIRECT_STRUCTURAL_CHILDREN("directStructuralChildren"),
        DIRECT_ACTION_DURATION("directActionDuration"),
        DIRECT_TRANSPORT_EVIDENCE("directTransportEvidence"),
        SET_NODE_DURATION("setNodeDuration"),
        AGGREGATE_CHILD_DURATIONS("aggregateChildDurations"),
        DURATION_DECISION("durationDecision"),
        AGGREGATE_NODE_DURATION("aggregateNodeDuration"),
        ALL_SCENARIO_CARDS("allScenarioCards"),
        AGGREGATE_VISIBLE_NODE_DURATIONS("aggregateVisibleNodeDurations"),
        AGGREGATE_NODE_DURATIONS("aggregateNodeDurations"),
        SCENARIO_CARD_FOR("scenarioCardFor"),
        NORMALIZE_CATEGORY_DURATIONS("normalizeCategoryDurations"),
        NORMALIZE_OVERVIEW_DURATION("normalizeOverviewDuration"),
        NORMALIZE_FAILS_DURATIONS("normalizeFailsDurations"),
        NORMALIZE_PROJECTED_DURATIONS("normalizeProjectedDurations"),
        MARK_DURATION("markDuration"),
        PUBLISH_DURATION("publishDuration"),
        CLASS_DURATION_SUMMARY("classDurationSummary"),
        PUBLISH_CLASS_DURATION("publishClassDuration"),
        AGGREGATE_CLASS_DURATIONS("aggregateClassDurations"),
        SCHEDULE_DURATION_AGGREGATION("scheduleDurationAggregation"),
        SCHEDULE_DURATION_AFTER_CLASS_CLICK("scheduleDurationAggregationAfterClassClick"),
        BIND_DURATION_CLASS_CLICKS("bindDurationClassClicks"),
        WATCH_DURATION_NODES("watchDurationNodes");

        private final String value;

        Function(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** Functions published by other runtime fragments and consumed as optional hooks. */
    public enum ExternalFunction {
        NORMALIZE_CATEGORY_LINKS("normalizeCategoryLinks"),
        CATEGORY_SCENARIO_INDEX("categoryScenarioIndex");

        private final String value;

        ExternalFunction(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static String attribute(Attribute attribute) {
        return Objects.requireNonNull(attribute, "attribute").value();
    }

    public static String selector(Selector selector) {
        return Objects.requireNonNull(selector, "selector").value();
    }

    public static String selectors(Selector... selectors) {
        return Arrays.stream(selectors)
                .filter(Objects::nonNull)
                .map(Selector::value)
                .collect(Collectors.joining(","));
    }

    public static String function(Function function) {
        return Objects.requireNonNull(function, "function").value();
    }

    public static String externalFunction(ExternalFunction function) {
        return Objects.requireNonNull(function, "function").value();
    }

    /** Emits a safe JavaScript string literal for a typed selector or attribute. */
    public static String jsString(String value) {
        return "'" + Objects.requireNonNull(value, "value")
                .replace("\\", "\\\\")
                .replace("'", "\\'") + "'";
    }
}
