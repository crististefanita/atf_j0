package atf.reporting.theme.tokens;

import java.util.Objects;

/**
 * Typed selector vocabulary shared by the report theme and UI contract.
 *
 * <p>Classes and ids are kept in one place so a renderer change cannot silently
 * leave the browser contract, CSS and static auditors talking about different
 * surfaces.</p>
 */
public final class AtfReportCssSelectors {
    private AtfReportCssSelectors() {
    }

    public enum Surface {
        TEST_WRAPPER(".test-wrapper", null),
        TEST_VIEW(".test-view", "#test-view"),
        CATEGORY_VIEW(".category-view", "#category-view"),
        DASHBOARD_VIEW(".dashboard-view", "#dashboard-view"),
        FAILS_VIEW(".atf-fails-view", "#atf-fails-view"),
        BODY("body", null);

        private final String selector;
        private final String hash;

        Surface(String selector, String hash) {
            this.selector = selector;
            this.hash = hash;
        }

        public String selector() {
            return selector;
        }

        /** Returns the surface selector itself constrained to visible DOM. */
        public String visible() {
            return selector + ":visible";
        }

        public String hash() {
            if (hash == null) {
                throw new IllegalStateException("Surface has no id/hash: " + name());
            }
            return hash;
        }
    }

    /**
     * Reusable structural elements inside a report surface.
     *
     * <p>Top-level navigation belongs to {@link Surface}; repeated report
     * building blocks belong here.  Keeping the two vocabularies separate
     * prevents a component selector from being mistaken for a navigable view
     * or an URL hash.</p>
     */
    public enum Component {
        TEST_LIST(".test-list"),
        TEST_LIST_WRAPPER(".test-list-wrapper"),
        TEST_LIST_ITEM(".test-list-item"),
        TEST_ITEM(".test-item"),
        TEST_CONTENT(".test-content"),
        TEST_CONTENT_DETAIL(".test-content-detail"),
        TEST_CONTENTS(".test-contents"),
        TEST_LIST_TOOLS(".test-list-tools"),
        TEST_DETAIL(".test-detail"),
        TEST_STATUS(".test-status"),
        DETAIL_BODY(".detail-body"),
        CARD(".card"),
        CARD_HEADER(".card-header"),
        CARD_BODY(".card-body"),
        CARD_TITLE(".card-title"),
        CARD_TOOLBAR(".card-toolbar"),
        NODE(".node"),
        SCENARIO_CARD(".card[data-atf-scenario-id]"),
        SCENARIO_HEADER(".card[data-atf-scenario-id]>.card-header"),
        SCENARIO_TITLE(".card[data-atf-scenario-id]>.card-header>.card-title"),
        SCENARIO_NODE(".card[data-atf-scenario-id]>.card-header>.card-title>a.node");

        private final String selector;

        Component(String selector) {
            this.selector = selector;
        }

        public String selector() {
            return selector;
        }

        /**
         * Attribute-form selector for the stable component class token.
         * The renderer keeps CSS shorthand, while browser tests bind to the
         * semantic class attribute instead.
         */
        public String attributeSelector() {
            if (!selector.matches("\\.[A-Za-z0-9_-]+")) {
                throw new IllegalStateException("Component is not a single class token: " + selector);
            }
            return "[class~='" + selector.substring(1) + "']";
        }

        public String attributeSelector(String element) {
            return Objects.requireNonNull(element, "element") + attributeSelector();
        }

        /** Returns this component constrained to visible DOM. */
        public String visible() {
            return selector + ":visible";
        }
    }

    /** Stable selectors used by the renderer-neutral fallback asset. */
    public enum Fallback {
        TAB(".tab"),
        TAB_PANEL(".tab-panel"),
        ACTIVE(".active"),
        FAIL_ROW(".fail-row");

        private final String selector;

        Fallback(String selector) {
            this.selector = selector;
        }

        public String selector() {
            return selector;
        }
    }

    public static String descendant(Surface parent, String child) {
        return Objects.requireNonNull(parent, "parent").selector() + " "
                + Objects.requireNonNull(child, "child");
    }

    public static String descendant(String parent, String child) {
        return Objects.requireNonNull(parent, "parent") + " "
                + Objects.requireNonNull(child, "child");
    }

    public static String descendant(Surface parent, Component child) {
        return descendant(parent, Objects.requireNonNull(child, "child").selector());
    }

    public static String descendant(Component parent, Component child) {
        return Objects.requireNonNull(parent, "parent").selector() + " "
                + Objects.requireNonNull(child, "child").selector();
    }

    public static String descendant(Component parent, String child) {
        return Objects.requireNonNull(parent, "parent").selector() + " "
                + Objects.requireNonNull(child, "child");
    }

    public static String directChild(String parent, String child) {
        return Objects.requireNonNull(parent, "parent") + ">"
                + Objects.requireNonNull(child, "child");
    }

    public static String compound(String first, String second) {
        return Objects.requireNonNull(first, "first")
                + Objects.requireNonNull(second, "second");
    }
}
