package atf.reporting.runtime.core;

import java.util.Objects;

/**
 * Stable browser contract consumed by the injected Extent runtime.
 *
 * <p>The runtime is emitted as JavaScript, but its DOM vocabulary is owned in
 * one typed Java boundary. This keeps selectors and attributes reusable by
 * component tests and prevents a renderer change from silently updating only
 * one controller.</p>
 */
public final class ExtentReportRuntimeVocabulary {
    public static final String DEFAULT_TEST_CLASS = "API tests";
    public static final String DEFAULT_PACKAGE = "Unspecified";
    public static final String DEFAULT_SCENARIO = "Scenario";

    private ExtentReportRuntimeVocabulary() {
    }

    /** Header elements which look adjacent to a duration but have another role. */
    public enum HeaderMarker {
        /** Vendor deep-link anchor; it identifies the node, not its duration. */
        URI_ANCHOR(".uri-anchor"),
        /** Vendor expand/collapse pointer; its minus glyph is a disclosure state. */
        DISCLOSURE_POINTER(".pointer");

        private final String selector;

        HeaderMarker(String selector) {
            this.selector = selector;
        }

        public String selector() {
            return selector;
        }
    }

    public enum Selector {
        CLASS_ITEMS(".test-list-item > .test-item[test-id]"),
        TEST_ITEM_HOST(".test-item"),
        DIRECT_SCENARIO_CARDS(".test-contents > .mt-4 > .accordion > .card"),
        TEST_DETAIL_NAME(".test-detail .name"),
        SCENARIO_TITLE(".card-title .node"),
        TEST_STATUS(".detail-head .test-status"),
        CARD_TOOLBAR_BADGE(".card-toolbar .badge"),
        DURATION_MARKERS(durationMarkers()),
        TAG_BADGES(".node-attr .badge-pill"),
        TEST_INFO(".test-contents .detail-head .info"),
        PACKAGE_NODE(":scope > .atf-test-package"),
        PACKAGE_FALLBACK_NODE(":scope > .m-t-10"),
        TEST_STATUS_NODE(":scope > .test-status");

        private final String value;

        Selector(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private static String durationMarkers() {
        return String.join(",",
                ".node-info .badge-default",
                ".test-list-item .text-sm span",
                ".test-detail .text-sm span",
                ".detail-head .info > .badge-default:not("
                        + HeaderMarker.URI_ANCHOR.selector() + "):not("
                        + HeaderMarker.DISCLOSURE_POINTER.selector() + ")");
    }

    public enum Attribute {
        TEST_ID("test-id"),
        SCENARIO_ID("data-atf-scenario-id"),
        CLASS_ROOT_ID("data-atf-class-root-id"),
        SCENARIO_INDEX("data-atf-scenario-index"),
        SCENARIO_TAGS("data-atf-scenario-tags"),
        TEST_PACKAGE("data-atf-test-package"),
        AUTHOR("author"),
        TAG("tag"),
        STATUS("status");

        private final String value;

        Attribute(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum StatusClass {
        DANGER("danger"),
        WARNING("warning"),
        PASS("pass");

        private final String value;

        StatusClass(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** Semantic status values returned by runtime predicates. */
    public enum Status {
        FAIL("fail", "fail"),
        WARNING("warning", "warn"),
        PASS("pass", "pass"),
        SKIP("skip", "skip");

        private final String value;
        private final String matchToken;

        Status(String value, String matchToken) {
            this.value = value;
            this.matchToken = matchToken;
        }

        public String value() {
            return value;
        }

        public String matchToken() {
            return matchToken;
        }
    }

    /** CSS classes emitted by the runtime when it has to create semantic nodes. */
    public enum CssClass {
        TEST_PACKAGE("atf-test-package");

        private final String value;

        CssClass(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** Stable names of runtime primitives shared by the injected controllers. */
    public enum Function {
        QUERY_ALL("q"),
        ESCAPE("esc"),
        TEXT("text"),
        CLASS_ITEMS("classItems"),
        DIRECT_SCENARIO_CARDS("directScenarioCards"),
        IS_CLASS_ROOT("isClassRoot"),
        TEST_ITEMS("testItems"),
        STATUS_OF("statusOf"),
        TEST_CLASS("testClass"),
        TEST_PACKAGE("testPackage"),
        TEST_NAME("testName"),
        READ_TAG_VALUES("readTagValues"),
        TEST_TAGS("testTags"),
        NORMALIZE_TEST_PACKAGE_METADATA("normalizeTestPackageMetadata");

        private final String value;

        Function(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** Function names published by another controller and consumed as hooks. */
    public enum ExternalFunction {
        CAPTURE_DOM_CONTRACT("captureDomContract");

        private final String value;

        ExternalFunction(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static String js(String value) {
        Objects.requireNonNull(value, "value");
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    public static String function(Function function) {
        return Objects.requireNonNull(function, "function").value();
    }

    public static String externalFunction(ExternalFunction function) {
        return Objects.requireNonNull(function, "function").value();
    }
}
