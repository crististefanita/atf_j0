package atf.ui.tests.extent.support.inspection.failure;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.ui.tests.extent.support.section.extract.OverviewSectionExtractor;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inspects one real failure journey without rebuilding the API report in the
 * browser layer: Tests → Fails → Open in Tests → Overview.
 *
 * <p>The selected scenario may remain visible as the navigation target, but
 * every phase, request/validation card and evidence disclosure must start
 * closed. This keeps failure navigation actionable without opening a wall of
 * technical trace on arrival.</p>
 */
public final class FailureFlowInspector {
    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;
    private final OverviewSectionExtractor overviewExtractor;

    public FailureFlowInspector(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = Objects.requireNonNull(auditFlow, "auditFlow");
        this.locators = Objects.requireNonNull(locators, "locators");
        this.overviewExtractor = new OverviewSectionExtractor(locators);
    }

    public Observation inspect(Page testsPage, String expectedOwner) {
        Objects.requireNonNull(testsPage, "testsPage");
        Objects.requireNonNull(expectedOwner, "expectedOwner");
        List<String> violations = new ArrayList<>();
        Locator owners = locators.testOwnerNames(locators.testsView(testsPage));
        List<String> ownerNames = new ArrayList<>();
        for (int index = 0; index < owners.count(); index++) {
            ownerNames.add(auditFlow.actions().getText(owners.nth(index),
                    "#test-view .test-list .test-item .name",
                    "Lifecycle API owner name is visible"));
        }
        boolean lifecycleOwnerPresent = ownerNames.size() == 1 && ownerNames.contains(expectedOwner);
        if (!lifecycleOwnerPresent) {
            violations.add("expected exactly one owner " + expectedOwner + " but observed " + ownerNames);
        }

        if (lifecycleOwnerPresent) {
            auditFlow.openTab(ReportUiContract.Tab.TESTS.hash());
            new atf.ui.tests.extent.support.api.ApiReportUiFlow(auditFlow, locators)
                    .openOwner(testsPage, expectedOwner);
        }
        CollapsedState initialState = collapsedState(testsPage);
        if (!initialState.detailsCollapsed()) {
            violations.add("initial Tests state is not collapsed: " + initialState.summary());
        }

        Page failsPage = auditFlow.openTab(ReportUiContract.Tab.FAILS.hash());
        Locator fails = locators.failsView(failsPage);
        int failureCount = locators.failureItems(fails).count();
        int primaryCauseCount = locators.failureCauseValues(fails).count();
        int openLinkCount = locators.failureOpenLinks(fails).count();
        boolean rootCausePrecedenceVisible = false;
        for (int index = 0; index < failureCount; index++) {
            auditFlow.actions().click(locators.failureItemAction(failsPage, index),
                    "inspect failure cause precedence for row " + (index + 1));
            boolean primary = locators.failureDetailPrimaryCauses(fails).count() > 0;
            boolean secondary = locators.failureDetailSecondaryCauses(fails).count() > 0;
            if (primary && secondary) {
                rootCausePrecedenceVisible = true;
                break;
            }
        }
        if (!rootCausePrecedenceVisible) {
            violations.add("Fails detail does not expose PRIMARY plus SECONDARY precedence");
        }
        boolean failureDeepLinkWorks = false;
        if (openLinkCount > 0) {
            auditFlow.actions().click(locators.firstFailureOpenAction(failsPage),
                    "first lifecycle failure opens in Tests");
            auditFlow.actions().waitForVisible(locators.testsView(failsPage),
                    ReportUiContract.Tab.TESTS.hash());
            failureDeepLinkWorks = locators.testsView(failsPage).isVisible()
                    && failsPage.url().contains("#test-id=")
                    && failsPage.url().contains("scenario-index=");
        }
        CollapsedState deepLinkState = collapsedState(failsPage);
        if (!deepLinkState.navigationTargetCollapsed()) {
            violations.add("Fails → Tests opens details: " + deepLinkState.summary());
        }

        Page overviewPage = auditFlow.openTab(ReportUiContract.Tab.OVERVIEW.hash());
        ReportAuditSnapshot.Overview overview = overviewExtractor.inspect(overviewPage, true);
        int overviewFailureCount = metricValue(overviewPage, "Scenario fail");
        if (overviewFailureCount <= 0) {
            violations.add("Overview does not expose a positive Scenario fail metric");
        }

        boolean valid = lifecycleOwnerPresent && failureCount > 0 && primaryCauseCount > 0
                && rootCausePrecedenceVisible && openLinkCount > 0 && failureDeepLinkWorks
                && initialState.detailsCollapsed()
                && deepLinkState.navigationTargetCollapsed() && overview.complete()
                && overviewFailureCount > 0;
        return new Observation(List.copyOf(ownerNames), lifecycleOwnerPresent, failureCount,
                primaryCauseCount, openLinkCount, rootCausePrecedenceVisible, failureDeepLinkWorks, initialState,
                deepLinkState, overviewPage, overviewFailureCount, valid, List.copyOf(violations));
    }

    private CollapsedState collapsedState(Page page) {
        Locator tests = locators.testsView(page);
        return new CollapsedState(
                locators.expandedScenarioHeaders(tests).count(),
                locators.expandedPhaseHeaders(tests).count(),
                locators.expandedRequestOrValidationHeaders(tests).count(),
                locators.openEvidenceDisclosures(tests).count());
    }

    private int metricValue(Page page, String label) {
        Locator metrics = locators.overviewMetrics(locators.overviewView(page));
        for (int index = 0; index < metrics.count(); index++) {
            Locator metric = metrics.nth(index);
            String actual = locators.metricLabel(metric).textContent();
            if (label.equals(actual == null ? "" : actual.trim())) {
                String value = locators.metricValue(metric).textContent();
                try {
                    return Integer.parseInt(value == null ? "-1" : value.trim());
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public record CollapsedState(int expandedScenarioCount, int expandedPhaseCount,
                                 int expandedRequestOrValidationCount,
                                 int openEvidenceCount) {
        public boolean detailsCollapsed() {
            return expandedScenarioCount == 0 && expandedPhaseCount == 0
                    && expandedRequestOrValidationCount == 0 && openEvidenceCount == 0;
        }

        /** A deep-link may keep its selected scenario open; descendants stay closed. */
        public boolean navigationTargetCollapsed() {
            return expandedScenarioCount <= 1 && expandedPhaseCount == 0
                    && expandedRequestOrValidationCount == 0 && openEvidenceCount == 0;
        }

        public String summary() {
            return "scenario=" + expandedScenarioCount + ", phase=" + expandedPhaseCount
                    + ", request/validation=" + expandedRequestOrValidationCount
                    + ", evidence=" + openEvidenceCount;
        }
    }

    public record Observation(List<String> ownerNames, boolean lifecycleOwnerPresent,
                              int failureCount, int primaryCauseCount, int openLinkCount,
                              boolean rootCausePrecedenceVisible, boolean failureDeepLinkWorks,
                              CollapsedState initialState,
                              CollapsedState deepLinkState, Page overviewPage,
                              int overviewFailureCount, boolean valid,
                              List<String> violations) {
        public Observation {
            ownerNames = List.copyOf(Objects.requireNonNull(ownerNames, "ownerNames"));
            initialState = Objects.requireNonNull(initialState, "initialState");
            deepLinkState = Objects.requireNonNull(deepLinkState, "deepLinkState");
            overviewPage = Objects.requireNonNull(overviewPage, "overviewPage");
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean detailsCollapsed() {
            return initialState.detailsCollapsed() && deepLinkState.navigationTargetCollapsed();
        }

        public String collapsedSummary() {
            return "initial[" + initialState.summary() + "] deep-link["
                    + deepLinkState.summary() + "]";
        }
    }
}
