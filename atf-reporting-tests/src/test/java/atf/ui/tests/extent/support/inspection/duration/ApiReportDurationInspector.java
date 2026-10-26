package atf.ui.tests.extent.support.inspection.duration;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

/** Orchestrates duration flows; observation, policy and geometry have separate owners. */
public final class ApiReportDurationInspector {
    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;
    private final ApiReportDurationDomInspector domInspector;
    private final ApiReportDurationResponsiveInspector responsiveInspector;

    public ApiReportDurationInspector(UiAuditFlow auditFlow) {
        this(auditFlow, new ReportUiLocators());
    }

    public ApiReportDurationInspector(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = auditFlow;
        this.locators = locators;
        this.domInspector = new ApiReportDurationDomInspector();
        this.responsiveInspector = new ApiReportDurationResponsiveInspector(auditFlow, locators);
    }

    public ReportAuditSnapshot.Duration selectOwnerAndInspectDuration(Page page, int index) {
        Locator detail = locators.visibleTestDetail(page);
        auditFlow.actions().click(locators.testOwnerItemAction(page, index),
                "duration owner is selected");
        auditFlow.actions().waitForVisible(detail, ReportUiContract.VISIBLE_DETAIL_SELECTOR);
        auditFlow.actions().waitForAttribute(page,
                new AtfLocator(locators.testsView(page),
                        ReportUiContract.Tab.TESTS.hash()),
                "data-atf-duration-ready", "true",
                "duration aggregation is ready after owner selection");
        return inspectDurationContract(page);
    }

    public ReportAuditSnapshot.Duration inspectDurationContract(Page page) {
        return domInspector.readDurationContract(page);
    }

    public List<ReportAuditSnapshot.DurationProjection> inspectDurationProjections(Page page) {
        return domInspector.readDurationProjections(page);
    }

    /** Compares status-badge geometry at normal and narrow viewports. */
    public ReportAuditSnapshot.BadgeGeometry compareStatusBadgeGeometry(Page page,
                                                                          int normalWidth,
                                                                          int normalHeight,
                                                                          int narrowWidth,
                                                                          int narrowHeight) {
        return responsiveInspector.compare(page, normalWidth, normalHeight, narrowWidth, narrowHeight);
    }
}
