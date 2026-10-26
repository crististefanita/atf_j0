package atf.ui.tests.extent.support.section.module;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.runtime.SectionAuditCheckExecutor;
import atf.reporting.audit.ValidationResult;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.reporting.ui.audit.section.CategoriesSectionValidator;
import atf.ui.tests.extent.support.audit.screen.CategoryOutcomeFlowAuditor;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.ui.contract.ReportUiContract;
import com.microsoft.playwright.Page;


/** Categories browser adapter; cardinality and styling are validated purely. */
public final class CategoriesSectionAuditModule implements ExtentSectionAuditModule {
    private final CategoriesSectionValidator validator = new CategoriesSectionValidator();
    private final SectionAuditCheckExecutor checkExecutor = new SectionAuditCheckExecutor();
    private final ReportUiLocators locators = new ReportUiLocators();

    @Override public ExtentSection section() { return ExtentSection.CATEGORIES; }

    @Override
    public SectionAuditResult audit(SectionAuditFixture fixture, SectionAuditSpec spec) {
        var filters = fixture.exploration().inspectFiltersAndFailures();
        ReportAuditSnapshot.Annotations snapshot = filters.annotations();
        ValidationResult result = (spec.hasCheck(ExtentSectionCheck.STRUCTURE)
                || spec.hasCheck(ExtentSectionCheck.STYLE))
                ? validator.validate(snapshot) : ValidationResult.empty();
        if (spec.hasCheck(ExtentSectionCheck.CROSS_SECTION)) {
            Page categoryPage = fixture.audit().openTab(ReportUiContract.Tab.CATEGORIES.hash());
            CategoryOutcomeFlowAuditor auditor = new CategoryOutcomeFlowAuditor(fixture.audit(), locators);
            CategoryOutcomeFlowAuditor.Observation outcomes = auditor.inspect(categoryPage);
            result = result.merge(auditor.validate(outcomes));
        }
        SectionAuditCheckExecutor.Execution execution = checkExecutor.execute(section(), spec, result);
        return new SectionAuditResult(section(), spec.checks(), execution.executedChecks(),
                spec.journey(), execution.validation(),
                java.util.List.of("category cardinalities", "filter surface"));
    }
}
