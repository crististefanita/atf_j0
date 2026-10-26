package atf.ui.tests.extent.support.section.module;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.runtime.SectionAuditCheckExecutor;
import atf.reporting.audit.ValidationResult;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.reporting.ui.audit.section.EvidenceSectionValidator;


/** Evidence browser adapter; request/response JSON is validated as one contract. */
public final class EvidenceSectionAuditModule implements ExtentSectionAuditModule {
    private final EvidenceSectionValidator validator = new EvidenceSectionValidator();
    private final SectionAuditCheckExecutor checkExecutor = new SectionAuditCheckExecutor();

    @Override public ExtentSection section() { return ExtentSection.EVIDENCE; }

    @Override
    public SectionAuditResult audit(SectionAuditFixture fixture, SectionAuditSpec spec) {
        var page = fixture.flow().openTests();
        ReportAuditSnapshot.RequestEvidence snapshot =
                fixture.exploration().inspectFirstRequestWithEvidence(page);
        ValidationResult result = (spec.hasCheck(ExtentSectionCheck.STRUCTURE)
                || spec.hasCheck(ExtentSectionCheck.JSON_CONTRACT))
                ? validator.validate(snapshot) : ValidationResult.empty();
        if (fixture.setup() != null && spec.hasCheck(
                ExtentSectionCheck.MOCK_RECONCILIATION)) {
            result = result.merge(fixture.reconcileMockContracts(page));
        }
        SectionAuditCheckExecutor.Execution execution = checkExecutor.execute(section(), spec, result);
        return new SectionAuditResult(section(), spec.checks(), execution.executedChecks(),
                spec.journey(), execution.validation(),
                java.util.List.of("request JSON", "response JSON", "execution evidence"));
    }
}
