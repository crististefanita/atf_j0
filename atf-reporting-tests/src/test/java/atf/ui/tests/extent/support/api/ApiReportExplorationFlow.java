package atf.ui.tests.extent.support.api;
import atf.ui.tests.extent.support.inspection.duration.ApiReportDurationInspector;
import atf.ui.tests.extent.support.inspection.projection.ReportProjectionMatrixInspector;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.ui.tests.extent.support.section.extract.ApiReportStructureExtractor;
import atf.ui.tests.extent.support.section.extract.EvidenceSectionExtractor;
import atf.ui.tests.extent.support.section.extract.FilterSectionExtractor;
import atf.ui.tests.extent.support.section.extract.UiValidationSectionExtractor;
import atf.ui.tests.extent.support.section.extract.OverviewSectionExtractor;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Objects;

/**
 * Semantic facade for auditing an API Extent report through the ATF debug UI.
 *
 * <p>The facade exposes only flows and validators. Immutable observations live
 * in {@link ReportAuditSnapshot}, so another renderer or report adapter can
 * consume the same facts without depending on this browser facade.</p>
 */
public final class ApiReportExplorationFlow {
    private final ApiReportStructureExtractor structureExtractor;
    private final ApiReportDurationInspector durationInspector;
    private final FilterSectionExtractor filterExtractor;
    private final EvidenceSectionExtractor evidenceExtractor;
    private final ReportProjectionMatrixInspector projectionMatrixInspector;
    private final UiValidationSectionExtractor uiValidationExtractor;
    private final OverviewSectionExtractor overviewSectionExtractor;

    public ApiReportExplorationFlow(UiAuditFlow auditFlow) {
        this(auditFlow, new ReportUiLocators());
    }

    public ApiReportExplorationFlow(UiAuditFlow auditFlow, ReportUiLocators locators) {
        Objects.requireNonNull(auditFlow, "auditFlow");
        this.structureExtractor = new ApiReportStructureExtractor(auditFlow, locators);
        this.durationInspector = new ApiReportDurationInspector(auditFlow, locators);
        this.filterExtractor = new FilterSectionExtractor(auditFlow, structureExtractor, locators);
        this.evidenceExtractor = new EvidenceSectionExtractor(auditFlow, locators);
        this.projectionMatrixInspector = new ReportProjectionMatrixInspector(locators);
        this.uiValidationExtractor = new UiValidationSectionExtractor(auditFlow, locators);
        this.overviewSectionExtractor = new OverviewSectionExtractor(locators);
    }

    public ReportAuditSnapshot.Tests inspectEveryTestClass(Page page) {
        return structureExtractor.extractTests(page);
    }

    public ReportAuditSnapshot.Duration selectOwnerAndInspectDuration(Page page, int index) {
        return durationInspector.selectOwnerAndInspectDuration(page, index);
    }

    public int ownerCount(Page page) { return structureExtractor.ownerCount(page); }

    public ReportAuditSnapshot.Duration inspectDurationContract(Page page) {
        return durationInspector.inspectDurationContract(page);
    }

    public List<ReportAuditSnapshot.DurationProjection> inspectDurationProjections(Page page) {
        return durationInspector.inspectDurationProjections(page);
    }

    public FiltersSnapshot inspectFiltersAndFailures() {
        FilterSectionExtractor.Extraction extraction = filterExtractor.extract();
        return new FiltersSnapshot(extraction.categoryPage(), extraction.failsPage(), extraction.snapshot());
    }

    public boolean openFirstFailureInTests(FiltersSnapshot snapshot) {
        return filterExtractor.openFirstFailureInTests(new FilterSectionExtractor.Extraction(
                snapshot.categoryPage(), snapshot.failsPage(), snapshot.snapshot()));
    }

    public ReportAuditSnapshot.Annotations inspectAnnotations(Page category) {
        return structureExtractor.extractAnnotations(category);
    }

    public ReportAuditSnapshot.Failures inspectFailures(Page fails) {
        return structureExtractor.extractFailures(fails);
    }

    public ReportAuditSnapshot.RequestEvidence inspectFirstRequestWithEvidence(Page page) {
        return evidenceExtractor.extractFirstRequestWithEvidence(page);
    }

    public ReportAuditSnapshot.UiValidation inspectUiValidation(Page page) {
        return uiValidationExtractor.inspect(page);
    }

    public ReportAuditSnapshot.Overview inspectOverviewSnapshot(Page page, boolean includeHierarchy) {
        return overviewSectionExtractor.inspect(page, includeHierarchy);
    }

    public ReportProjectionMatrixInspector.Observation inspectProjection(Page page,
            ReportUiContract.Tab tab) {
        return projectionMatrixInspector.inspect(page, tab);
    }

    public ReportAuditSnapshot.Duration combine(ReportAuditSnapshot.Duration left,
                                                ReportAuditSnapshot.Duration right) {
        return left.plus(right);
    }

    /** Compares status-badge geometry at normal and narrow viewports. */
    public ReportAuditSnapshot.BadgeGeometry compareStatusBadgeGeometry(Page page,
            int normalWidth, int normalHeight, int narrowWidth, int narrowHeight) {
        return durationInspector.compareStatusBadgeGeometry(page, normalWidth, normalHeight,
                narrowWidth, narrowHeight);
    }

    /** Page handles remain flow concerns; the observations inside are neutral snapshots. */
    public record FiltersSnapshot(Page categoryPage, Page failsPage,
                                  ReportAuditSnapshot.Filters snapshot) {
        public ReportAuditSnapshot.Annotations annotations() { return snapshot.annotations(); }
        public ReportAuditSnapshot.Failures failures() { return snapshot.failures(); }
        public boolean tagLinks() { return snapshot.tagLinks(); }
        public boolean failureLinks() { return snapshot.failureLinks(); }
    }
}
