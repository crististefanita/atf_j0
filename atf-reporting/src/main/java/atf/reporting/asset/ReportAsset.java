package atf.reporting.asset;

import java.util.Objects;

/** Classpath-owned browser asset with a stable semantic owner and format. */
public enum ReportAsset {
    SCREENSHOT_VIEWER_CSS("/atf/reporting/css/screenshot-viewer.css", Format.CSS, "screenshot-viewer-style"),
    SCREENSHOT_VIEWER_JS("/atf/reporting/javascript/screenshot-viewer.js", Format.JAVASCRIPT, "screenshot-viewer"),
    TABLE_VIEWPORT_CSS("/atf/reporting/css/table-viewport.css", Format.CSS, "table-viewport"),
    TABLE_VIEWPORT_JS("/atf/reporting/javascript/table-viewport.js", Format.JAVASCRIPT, "table-viewport-keyboard"),
    FALLBACK_CSS("/atf/reporting/css/fallback.css", Format.CSS, "fallback-theme"),
    EVIDENCE_INTERACTION_CSS("/atf/reporting/css/evidence-interaction.css", Format.CSS,
            "evidence-interaction"),
    EVIDENCE_TABLE_CSS("/atf/reporting/css/evidence-table.css", Format.CSS,
            "evidence-table"),
    EVIDENCE_DISCLOSURE_CSS("/atf/reporting/css/evidence-disclosure.css", Format.CSS,
            "evidence-disclosure"),
    EVIDENCE_STACK_CSS("/atf/reporting/css/evidence-stack.css", Format.CSS,
            "evidence-stack"),
    UI_EVIDENCE_CSS("/atf/reporting/css/ui-evidence.css", Format.CSS,
            "ui-evidence"),
    EXTENT_OVERVIEW_LAYOUT_CSS("/atf/reporting/css/overview-layout.css", Format.CSS,
            "extent-overview-layout"),
    EXTENT_OVERVIEW_TAG_AUDIT_CSS("/atf/reporting/css/overview-tag-audit.css", Format.CSS,
            "extent-overview-tag-audit"),
    EXTENT_OVERVIEW_CATEGORY_TABLES_CSS("/atf/reporting/css/overview-category-tables.css", Format.CSS,
            "extent-overview-category-tables"),
    EXTENT_OVERVIEW_CATEGORY_STATUS_CSS("/atf/reporting/css/overview-category-status.css", Format.CSS,
            "extent-overview-category-status"),
    EXTENT_THEME_RESPONSIVE_CSS("/atf/reporting/css/theme-responsive.css", Format.CSS,
            "extent-theme-responsive"),
    EXTENT_THEME_BASE_CSS("/atf/reporting/css/theme-base.css", Format.CSS,
            "extent-theme-base"),
    EXTENT_THEME_CATEGORY_STATUS_CSS("/atf/reporting/css/theme-category-status.css", Format.CSS,
            "extent-theme-category-status"),
    EXTENT_THEME_FAILS_LAYOUT_CSS("/atf/reporting/css/theme-fails-layout.css", Format.CSS,
            "extent-theme-fails-layout"),
    EXTENT_THEME_FAILS_STATUS_CSS("/atf/reporting/css/theme-fails-status.css", Format.CSS,
            "extent-theme-fails-status"),
    EXTENT_THEME_CATEGORY_CSS("/atf/reporting/css/theme-category.css", Format.CSS,
            "extent-theme-category"),
    EXTENT_THEME_NAVIGATION_CSS("/atf/reporting/css/theme-navigation.css", Format.CSS,
            "extent-theme-navigation"),
    EXTENT_THEME_SCENARIO_CSS("/atf/reporting/css/theme-scenario.css", Format.CSS,
            "extent-theme-scenario"),
    EXTENT_THEME_DETAIL_CSS("/atf/reporting/css/theme-detail.css", Format.CSS,
            "extent-theme-detail"),
    EXTENT_TEST_FILTERS_JS("/atf/reporting/javascript/extent-tests-filters.js", Format.JAVASCRIPT,
            "extent-tests-filters"),
    EXTENT_NAVIGATION_DOM_JS("/atf/reporting/javascript/extent-navigation-dom.js", Format.JAVASCRIPT,
            "extent-navigation-dom"),
    EXTENT_NAVIGATION_DISCLOSURE_JS("/atf/reporting/javascript/extent-navigation-disclosure.js", Format.JAVASCRIPT,
            "extent-navigation-disclosure"),
    EXTENT_NAVIGATION_PERFORMANCE_JS("/atf/reporting/javascript/extent-navigation-performance.js", Format.JAVASCRIPT,
            "extent-navigation-performance"),
    EXTENT_NAVIGATION_CONTROLLER_JS("/atf/reporting/javascript/extent-navigation-controller.js", Format.JAVASCRIPT,
            "extent-navigation-controller"),
    EXTENT_OVERVIEW_AUDIT_JS("/atf/reporting/javascript/extent-overview-audit.js", Format.JAVASCRIPT,
            "extent-overview-audit"),
    EXTENT_OVERVIEW_LABELS_JS("/atf/reporting/javascript/extent-overview-labels.js", Format.JAVASCRIPT,
            "extent-overview-labels"),
    EXTENT_OVERVIEW_CHARTS_JS("/atf/reporting/javascript/extent-overview-charts.js", Format.JAVASCRIPT,
            "extent-overview-charts"),
    EXTENT_OVERVIEW_CLEANUP_JS("/atf/reporting/javascript/extent-overview-cleanup.js", Format.JAVASCRIPT,
            "extent-overview-cleanup"),
    EXTENT_SCENARIOS_JS("/atf/reporting/javascript/extent-scenarios.js", Format.JAVASCRIPT,
            "extent-scenarios"),
    EXTENT_CATEGORIES_JS("/atf/reporting/javascript/extent-categories.js", Format.JAVASCRIPT,
            "extent-categories"),
    FALLBACK_NAVIGATION_JS("/atf/reporting/javascript/fallback-navigation.js", Format.JAVASCRIPT,
            "fallback-navigation"),
    EXTENT_EVIDENCE_ROLES_JS("/atf/reporting/javascript/extent-evidence-roles.js", Format.JAVASCRIPT,
            "extent-evidence-roles"),
    EXTENT_EVIDENCE_DISCLOSURE_JS("/atf/reporting/javascript/extent-evidence-disclosure.js", Format.JAVASCRIPT,
            "extent-evidence-disclosure"),
    EXTENT_EVIDENCE_FAILURES_JS("/atf/reporting/javascript/extent-evidence-failures.js", Format.JAVASCRIPT,
            "extent-evidence-failures"),
    EXTENT_EVIDENCE_TABLE_LAYOUT_JS("/atf/reporting/javascript/extent-evidence-table-layout.js", Format.JAVASCRIPT,
            "extent-evidence-table-layout"),
    EXTENT_FAILURE_DETECTION_JS("/atf/reporting/javascript/extent-failure-detection.js", Format.JAVASCRIPT,
            "extent-failure-detection"),
    EXTENT_FAILURE_CONTEXT_JS("/atf/reporting/javascript/extent-failure-context.js", Format.JAVASCRIPT,
            "extent-failure-context"),
    EXTENT_FAILURE_CAUSE_CHAIN_JS("/atf/reporting/javascript/extent-failure-cause-chain.js", Format.JAVASCRIPT,
            "extent-failure-cause-chain"),
    EXTENT_FAILURE_PROJECTION_JS("/atf/reporting/javascript/extent-failure-projection.js", Format.JAVASCRIPT,
            "extent-failure-projection"),
    RUNTIME_BOOT_JS("/atf/reporting/javascript/runtime-boot.js", Format.JAVASCRIPT,
            "runtime-boot"),
    RUNTIME_CORE_DOM_JS("/atf/reporting/javascript/runtime-core-dom.js", Format.JAVASCRIPT,
            "runtime-core-dom"),
    RUNTIME_CORE_SCENARIOS_JS("/atf/reporting/javascript/runtime-core-scenarios.js", Format.JAVASCRIPT,
            "runtime-core-scenarios"),
    RUNTIME_CORE_METADATA_JS("/atf/reporting/javascript/runtime-core-metadata.js", Format.JAVASCRIPT,
            "runtime-core-metadata"),
    DURATION_PARSING_JS("/atf/reporting/javascript/duration-parsing.js", Format.JAVASCRIPT,
            "duration-parsing"),
    DURATION_DOM_JS("/atf/reporting/javascript/duration-dom.js", Format.JAVASCRIPT,
            "duration-dom"),
    DURATION_MEMORY_JS("/atf/reporting/javascript/duration-memory.js", Format.JAVASCRIPT,
            "duration-memory"),
    DURATION_AGGREGATION_JS("/atf/reporting/javascript/duration-aggregation.js", Format.JAVASCRIPT,
            "duration-aggregation"),
    DURATION_PROJECTION_LOOKUP_JS("/atf/reporting/javascript/duration-projection-lookup.js", Format.JAVASCRIPT,
            "duration-projection-lookup"),
    DURATION_PROJECTION_VIEWS_JS("/atf/reporting/javascript/duration-projection-views.js", Format.JAVASCRIPT,
            "duration-projection-views"),
    DURATION_PROJECTION_PUBLICATION_JS("/atf/reporting/javascript/duration-projection-publication.js", Format.JAVASCRIPT,
            "duration-projection-publication"),
    DURATION_SCHEDULING_JS("/atf/reporting/javascript/duration-scheduling.js", Format.JAVASCRIPT,
            "duration-scheduling"),
    DURATION_LIFECYCLE_JS("/atf/reporting/javascript/duration-lifecycle.js", Format.JAVASCRIPT,
            "duration-lifecycle"),
    DISCLOSURE_INDICATOR_CSS("/atf/reporting/css/disclosure-indicator.css", Format.CSS, "disclosure-indicator");

    private final String resourcePath;
    private final Format format;
    private final String owner;

    ReportAsset(String resourcePath, Format format, String owner) {
        this.resourcePath = resourcePath;
        this.format = format;
        this.owner = owner;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public Format format() {
        return format;
    }

    public String owner() {
        return owner;
    }

    public enum Format {
        CSS("text/css", ".css"),
        JAVASCRIPT("text/javascript", ".js");

        private final String mediaType;
        private final String extension;

        Format(String mediaType, String extension) {
            this.mediaType = mediaType;
            this.extension = extension;
        }

        public String mediaType() {
            return mediaType;
        }

        public String extension() {
            return extension;
        }
    }

    public static ReportAsset require(ReportAsset asset) {
        return Objects.requireNonNull(asset, "asset");
    }
}







