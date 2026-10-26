package atf.ui.tests.extent.support.section.module;
import atf.ui.tests.extent.support.inspection.failure.FailureFlowInspector;
import atf.reporting.ui.audit.runtime.SectionAuditCheckExecutor;
import atf.reporting.audit.ValidationResult;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ExtentJourney;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.reporting.ui.audit.section.FailsSectionValidator;
import com.microsoft.playwright.Page;

import java.util.List;

import static atf.reporting.audit.ReportAuditChecks.check;

/** Browser adapter for Fails; causes and deep-links are observed, not invented. */
public final class FailsSectionAuditModule implements ExtentSectionAuditModule {
    private final FailsSectionValidator validator = new FailsSectionValidator();
    private final SectionAuditCheckExecutor checkExecutor = new SectionAuditCheckExecutor();

    @Override
    public ExtentSection section() {
        return ExtentSection.FAILS;
    }

    @Override
    public SectionAuditResult audit(SectionAuditFixture fixture, SectionAuditSpec spec) {
        var snapshot = fixture.exploration().inspectFiltersAndFailures();
        ReportAuditSnapshot.Failures failures = snapshot.failures();
        ValidationResult result = spec.hasCheck(
                ExtentSectionCheck.STRUCTURE)
                ? fixture.setup() == null
                ? validator.validate(failures)
                : validator.validate(failures, fixture.expectedProjection())
                : ValidationResult.empty();
        if (spec.journey() == ExtentJourney.FAILURE_NAVIGATION) {
            if (failures.failureCount() == 0) {
                result = result.merge(ValidationResult.of(List.of(
                        check("Failure navigation input", "> 0",
                                "0", false))));
            } else {
                Page testsPage = fixture.flow().openTests();
                String expectedOwner = fixture.expectedOwnerNames().stream().findFirst()
                        .orElseGet(() -> fixture.flow().ownerNames(testsPage).stream()
                                .findFirst().orElseThrow(() -> new IllegalStateException(
                                        "Failure navigation requires one rendered owner")));
                FailureFlowInspector.Observation journey = new FailureFlowInspector(
                        fixture.audit(), fixture.flow().locators()).inspect(testsPage, expectedOwner);
                result = result.merge(ValidationResult.of(List.of(
                        check("Tests deep-link", "Tests + scenario-index",
                                String.valueOf(journey.failureDeepLinkWorks()),
                                journey.failureDeepLinkWorks()),
                        check("Collapsed details", "0 expanded",
                                journey.collapsedSummary(), journey.detailsCollapsed()),
                        check("Overview failure metric", "> 0",
                                String.valueOf(journey.overviewFailureCount()),
                                journey.overviewFailureCount() > 0))));
            }
        }
        SectionAuditCheckExecutor.Execution execution = checkExecutor.execute(section(), spec, result);
        return new SectionAuditResult(section(), spec.checks(), execution.executedChecks(),
                spec.journey(), execution.validation(),
                List.of("Fails rows", "failure cause/deep-link journey"));
    }
}

