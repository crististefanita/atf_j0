package atf.reporting.markup.vocabulary;

import atf.reporting.contract.AtfReportStatus;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Typed vocabulary for repeated semantic HTML/CSS tokens emitted by the report renderer.
 *
 * <p>The renderer owns the markup; this vocabulary owns the stable contract consumed by
 * static auditors and browser adapters. Keeping the tokens here prevents drift caused by
 * repeated raw strings while leaving report labels in {@code AtfReportLabels}.</p>
 */
public final class AtfReportHtmlVocabulary {
    private AtfReportHtmlVocabulary() {
    }

    /** Finite HTML shell vocabulary used by the renderer builders. */
    public enum Tag {
        HTML("html"),
        HEAD("head"),
        BODY("body"),
        META("meta"),
        TITLE("title"),
        STYLE("style"),
        SCRIPT("script"),
        NAV("nav"),
        HEADER("header"),
        MAIN("main"),
        ASIDE("aside"),
        DIV("div"),
        SECTION("section"),
        ARTICLE("article"),
        DETAILS("details"),
        SUMMARY("summary"),
        PRE("pre"),
        P("p"),
        H1("h1"),
        OL("ol"),
        UL("ul"),
        LI("li"),
        H2("h2"),
        H3("h3"),
        BUTTON("button"),
        A("a"),
        SPAN("span"),
        SMALL("small"),
        STRONG("strong"),
        I("i"),
        LINK("link"),
        TABLE("table"),
        COLGROUP("colgroup"),
        COL("col"),
        THEAD("thead"),
        TBODY("tbody"),
        TR("tr"),
        TH("th"),
        TD("td");

        private final String value;

        Tag(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum CssClass {
        ASSERTIONS("assertions"),
        ACTIVE("active"),
        ATF_SETUP_EVIDENCE("atf-setup-evidence"),
        ATF_SETUP_EVIDENCE_TABLE("atf-setup-evidence-table"),
        ATF_FAILS_LIST("atf-fails-list"),
        ATF_FAILS_VIEW("atf-fails-view"),
        ATF_SECONDARY_BODY("atf-secondary-body"),
        ATF_SECONDARY_GRID("atf-secondary-grid"),
        ATF_SECONDARY_OVERVIEW("atf-secondary-overview"),
        ATF_SECONDARY_OVERVIEW_BADGE("atf-secondary-overview-badge"),
        ATF_SECONDARY_OVERVIEW_TITLE("atf-secondary-overview-title"),
        ATF_TAG_AUDIT_BODY("atf-tag-audit-body"),
        ATF_TAG_AUDIT_DISCLOSURE("atf-tag-audit-disclosure"),
        ATF_TAG_GRID("atf-tag-grid"),
        ATF_TAG_SCROLL("atf-tag-scroll"),
        ATF_TAG_TABLE("atf-tag-table"),
        ATF_TAG_CARD("atf-tag-card"),
        ATF_UI_ACTION_IDENTITY("atf-ui-action-identity"),
        ATF_UI_ACTION_OPERATION("atf-ui-action-operation"),
        ATF_UI_ACTION_SEQUENCE("atf-ui-action-sequence"),
        ATF_UI_ACTION_NUMBER("atf-ui-action-number"),
        ATF_UI_ACTION_SCREENSHOT("atf-ui-action-screenshot"),
        ATF_UI_ACTION_SCREENSHOT_LINK("atf-ui-action-screenshot-link"),
        ATF_UI_SCREENSHOT_DUPLICATE("atf-ui-screenshot-duplicate"),
        ATF_UI_SCREENSHOT_DUPLICATE_TEXT("atf-ui-screenshot-duplicate-text"),
        ATF_UI_ACTION_TABLE("atf-ui-action-table"),
        ATF_UI_ACTION_TABLE_WRAP("atf-ui-action-table-wrap"),
        ATF_UI_ACTION_TIME("atf-ui-action-time"),
        ATF_UI_ACTION_RESULT("atf-ui-action-result"),
        ATF_UI_OBSERVATIONS("atf-ui-observations"),
        ATF_UI_OBSERVATION_TABLE("atf-ui-observation-table"),
        ATF_UI_ASSERTIONS("atf-ui-assertions"),
        ATF_VALIDATION_TABLE_WRAP("atf-validation-table-wrap"),
        ATF_UI_EVIDENCE_SCREENSHOT_LINK("atf-ui-evidence-screenshot-link"),
        ATF_EMPTY_STATE("atf-empty-state"),
        ATF_EMPTY_STATE_TITLE("atf-empty-state-title"),
        ATF_EMPTY_STATE_COPY("atf-empty-state-copy"),
        ATF_OVERVIEW_DEFAULT("atf-overview-default"),
        BADGE("badge"),
        BADGE_PRIMARY("badge-primary"),
        BACK_TO_TEST("back-to-test"),
        BRAND("brand"),
        CATEGORY_VIEW("category-view"),
        DASHBOARD_VIEW("dashboard-view"),
        CONTAINER_FLUID("container-fluid"),
        P4("p-4"),
        TEST_VIEW("test-view"),
        TITLE("title"),
        SPA("spa"),
        REPORT("-report"),
        DARK("dark"),
        BADGE_GRADIENT_PRIMARY("badge-gradient-primary"),
        NAV_ITEM("nav-item"),
        DROPDOWN("dropdown"),
        DROPDOWN_TOGGLE("dropdown-toggle"),
        ICO("ico"),
        EXCEPTION_VIEW("exception-view"),
        OPEN("open"),
        TABLE("table"),
        TABLE_SM("table-sm"),
        CARD("card"),
        CARD_HEADER("card-header"),
        CARD_BODY("card-body"),
        EVIDENCE_KEY_COLUMN("atf-evidence-key-column"),
        EVIDENCE_VALUE_COLUMN("atf-evidence-value-column"),
        EVIDENCE_URL_VALUE("atf-evidence-url-value"),
        DISCLOSURE_GLYPH("atf-disclosure-glyph"),
        EXECUTION_EVIDENCE("execution-evidence"),
        EXECUTION_EVIDENCE_BODY("execution-evidence-body"),
        EXECUTION_TABLE("execution-table"),
        HELM_API_CLASSIFICATION("helm-api-classification"),
        HELM_API_CLASSIFICATION_LABEL("helm-api-classification-label"),
        HELM_EVIDENCE_TABLE("helm-evidence-table"),
        HELM_REQUEST_TABLE("helm-request-table"),
        HELM_VALIDATION_TABLE("helm-validation-table"),
        HELM_REQUEST_DETAILS("helm-request-details"),
        HELM_RESPONSE_DETAILS("helm-response-details"),
        HELM_TRACE_TABLE("helm-trace-table"),
        ATF_TRACE_TABLE_WRAP("atf-trace-table-wrap"),
        ATF_API_TABLE_WRAP("atf-api-table-wrap"),
        HELM_SCROLL_BLOCK_TITLE("helm-scroll-block-title"),
        HELM_SCROLL_BLOCK_BODY("helm-scroll-block-body"),
        JSON_CODE("json-code"),
        LAYOUT("layout"),
        FAIL_ROW("fail-row"),
        NODE_TITLE("node-title"),
        PHASE("phase"),
        PHASE_DETAILS("phase-details"),
        PHASE_DIAGNOSTICS("phase-diagnostics"),
        REQUEST("request"),
        REQUEST_DETAILS_CONTENT("request-details-content"),
        REQUEST_EVIDENCE("request-evidence"),
        REQUEST_JSON_BODY("request-json-body"),
        RESPONSE_EVIDENCE("response-evidence"),
        RESPONSE_JSON_BODY("response-json-body"),
        RESPONSE_DETAILS_CONTENT("response-details-content"),
        RETRY_HISTORY_TABLE("retry-history-table"),
        SCENARIO("scenario"),
        SCENARIO_TITLE("scenario-title"),
        SE_TITLE("se-title"),
        SETUP_EVIDENCE("setup-evidence"),
        STATUS("status"),
        STEP_DETAIL("step-detail"),
        TAG_LIST("tag-list"),
        TAG("tag"),
        SUMMARY("summary"),
        METRIC("metric"),
        CONTRACT("contract"),
        FAIL_LIST("fail-list"),
        EMPTY("empty"),
        TECHNICAL("technical"),
        ATTRIBUTES_VIEW("attributes-view"),
        TEST_WRAPPER("test-wrapper"),
        ROW("row"),
        VIEW("view"),
        HIDE("hide"),
        TEST_LIST("test-list"),
        TEST_LIST_TOOLS("test-list-tools"),
        TOOLS("tools"),
        PULL_LEFT("pull-left"),
        TEXT_RIGHT("text-right"),
        FONT_SIZE_14("font-size-14"),
        TEST_LIST_WRAPPER("test-list-wrapper"),
        SCROLLABLE("scrollable"),
        TEST_LIST_ITEM("test-list-item"),
        TEST_CONTENT_TOOLS("test-content-tools"),
        TEST_CONTENT_DETAIL("test-content-detail"),
        DETAIL_BODY("detail-body"),
        FA("fa"),
        FA_ARROW_LEFT("fa-arrow-left"),
        TAB("tab"),
        TABS("tabs"),
        TAB_PANEL("tab-panel"),
        TEST_CONTENT("test-content"),
        TEST_LINK("test-link"),
        VALIDATION("validation"),
        BODY("body");

        private final String value;

        CssClass(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Attribute {
        HREF("href"),
        SRC("src"),
        LANG("lang"),
        CHARSET("charset"),
        NAME("name"),
        CONTENT("content"),
        TARGET("target"),
        REL("rel"),
        STYLE("style"),
        TITLE("title"),
        ARIA_LABEL("aria-label"),
        ARIA_EXPANDED("aria-expanded"),
        ARIA_HIDDEN("aria-hidden"),
        ROLE("role"),
        ONCLICK("onclick"),
        FEATHERLIGHT("data-featherlight"),
        INLINE("data-atf-inline"),
        EVIDENCE_STATUS("data-atf-evidence-status"),
        EVIDENCE_OWNER("data-atf-evidence-owner"),
        REQUEST_METHOD("data-atf-request-method"),
        REQUEST_PATH("data-atf-request-path"),
        REQUEST_BODY_AVAILABILITY("data-atf-request-body-availability"),
        RESPONSE_BODY_AVAILABILITY("data-atf-response-body-availability"),
        EVIDENCE_TYPE("data-atf-evidence-type"),
        NODE_ROLE("data-atf-node-role"),
        EMPTY_STATE("data-atf-empty-state"),
        PHASE_NAME("data-atf-phase-name"),
        DIAGNOSTIC_STATUS("data-atf-diagnostic-status"),
        SCREENSHOT("data-atf-screenshot"),
        SCREENSHOT_REF("data-atf-screenshot-ref"),
        TAB("data-atf-tab"),
        FALLBACK_TAB("data-tab");

        private final String value;

        Attribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum AttributeValue {
        BASE64("base64"),
        IMAGE("image"),
        BLANK("_blank"),
        NOOPENER("noopener"),
        TRUE("true"),
        FALSE("false");

        private final String value;

        AttributeValue(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** Stable icon names kept at the vendor boundary instead of scattered literals. */
    public enum Icon {
        BUG("fa-bug"),
        BAR_CHART("fa-bar-chart");

        private final String value;

        Icon(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum ViewId {
        OVERVIEW("dashboard-view"),
        TESTS("test-view"),
        CATEGORIES("category-view"),
        FAILS("atf-fails-view");

        private final String value;

        ViewId(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum TabKey {
        OVERVIEW("overview"),
        TESTS("test"),
        CATEGORIES("category"),
        FAILS("fails");

        private final String value;

        TabKey(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Navigation vocabulary of the standalone HTML fallback.
     *
     * <p>The Extent document has its own vendor-facing tab ids, while the
     * fallback document is renderer-neutral. Keeping the fallback ids typed
     * prevents the HTML views, CSS and navigation asset from drifting apart.</p>
     */
    public enum FallbackTab {
        TESTS("tests", "Tests"),
        CATEGORY("category", "Category"),
        FAILS("fails", "Fails"),
        OVERVIEW("overview", "Overview");

        private final String value;
        private final String label;

        FallbackTab(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String value() {
            return value;
        }

        public String label() {
            return label;
        }
    }

    public enum ElementId {
        THEME_STYLE("atf-report-theme"),
        NAV_TEST("nav-test"),
        NAV_CATEGORY("nav-category"),
        NAV_FAILS("nav-fails"),
        NAV_OVERVIEW("nav-overview"),
        EXECUTION_METADATA("atf-execution-metadata"),
        EXECUTION_METADATA_GRID("atf-execution-metadata-grid"),
        TAG_AUDIT("atf-tag-audit"),
        FAILURE_LIST("atf-failure-list"),
        FAILURE_DETAIL("atf-failure-detail"),
        UI_EVIDENCE("atf-ui-exploratory-evidence"),
        UI_EVIDENCE_STYLE("atf-ui-evidence-style");

        private final String value;

        ElementId(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static String classAttribute(CssClass... classes) {
        return classAttribute(Arrays.stream(classes)
                .filter(Objects::nonNull)
                .map(CssClass::value)
                .toArray(String[]::new));
    }

    public static String classAttribute(String... classes) {
        return "class=\"" + classList(classes) + "\"";
    }

    public static String classList(String... classes) {
        return Arrays.stream(classes)
                .filter(Objects::nonNull)
                .map(AtfReportHtmlVocabulary::escape)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    public static String classList(CssClass... classes) {
        String[] values = Arrays.stream(classes)
                .filter(Objects::nonNull)
                .map(CssClass::value)
                .toArray(String[]::new);
        return classList(values);
    }

    public static String attribute(Attribute attribute, String value) {
        return attribute.value() + "=\"" + escapeAttribute(value) + "\"";
    }

    public static String attribute(Attribute attribute, AttributeValue value) {
        return attribute(attribute, Objects.requireNonNull(value, "value").value());
    }

    /** Serializes an already-escaped attribute list without introducing raw tag names in views. */
    public static String openTag(Tag tag, String... attributes) {
        Objects.requireNonNull(tag, "tag");
        String suffix = Arrays.stream(attributes == null ? new String[0] : attributes)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
        return "<" + tag.value() + (suffix.isBlank() ? "" : " " + suffix) + ">";
    }

    public static String closeTag(Tag tag) {
        return "</" + Objects.requireNonNull(tag, "tag").value() + ">";
    }

    /** Builds a semantic element while keeping the renderer's content opaque. */
    public static String element(Tag tag, String content, String... attributes) {
        return openTag(tag, attributes) + Objects.requireNonNullElse(content, "") + closeTag(tag);
    }

    /** Builds a void HTML element such as {@code meta}, without inventing a slash style. */
    public static String voidElement(Tag tag, String... attributes) {
        return openTag(tag, attributes);
    }

    public static String idAttribute(String id) {
        return "id=\"" + escape(id) + "\"";
    }

    public static String idAttribute(ViewId id) {
        return idAttribute(Objects.requireNonNull(id, "id").value());
    }

    public static String idAttribute(ElementId id) {
        return idAttribute(Objects.requireNonNull(id, "id").value());
    }

    public static String[] statusClasses(AtfReportStatus status) {
        return new String[]{CssClass.STATUS.value(), statusClass(status)};
    }

    public static String statusClass(AtfReportStatus status) {
        return stateClass("status-", status);
    }

    public static String statusValue(AtfReportStatus status) {
        return Objects.requireNonNull(status, "status").displayValue();
    }

    /** Canonical human-readable status text shared by fallback HTML views. */
    public static String statusLabel(AtfReportStatus status) {
        return AtfReportStatus.orInfo(status).displayLabel();
    }

    public static String statusKey(boolean failed) {
        return failed ? "status-fail" : "status-pass";
    }

    public static String stateClass(String prefix, AtfReportStatus status) {
        return Objects.requireNonNull(prefix, "prefix")
                + statusValue(status);
    }

    /** Escapes user/test evidence before it is emitted into HTML text or attributes. */
    public static String escapeHtml(String value) {
        return escape(value);
    }

    private static String escape(String value) {
        return Objects.requireNonNullElse(value, "")
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;");
    }

    /** Double-quoted attributes do not require apostrophe encoding. */
    private static String escapeAttribute(String value) {
        return escape(value).replace("&#39;", "'");
    }
}
