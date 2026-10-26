package atf.ui.tests.extent.support.section.module;
import atf.reporting.ui.audit.runtime.SectionAuditCheckExecutor;
import atf.reporting.audit.ValidationResult;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.reporting.ui.audit.section.TestsSectionValidator;
import atf.ui.tests.extent.support.audit.screen.ApiReportDurationAuditor;
import com.microsoft.playwright.Page;

import java.util.List;

/** Browser adapter for Tests; expectations remain in the pure validator. */
public final class TestsSectionAuditModule implements ExtentSectionAuditModule {
    private final TestsSectionValidator validator = new TestsSectionValidator();
    private final SectionAuditCheckExecutor checkExecutor = new SectionAuditCheckExecutor();
    private final ApiReportDurationAuditor durationAuditor = new ApiReportDurationAuditor();

    @Override
    public ExtentSection section() {
        return ExtentSection.TESTS;
    }

    @Override
    public SectionAuditResult audit(SectionAuditFixture fixture, SectionAuditSpec spec) {
        Page page = fixture.flow().openTests();
        ReportAuditSnapshot.Tests snapshot = fixture.exploration().inspectEveryTestClass(page);
        ValidationResult result = fixture.setup() == null
                || fixture.setup().selectedTests().isEmpty()
                ? validator.validate(snapshot, fixture.expectedOwnerNames())
                : validator.validate(snapshot, fixture.expectedOwnerNames(),
                        fixture.expectedProjection());
        if (fixture.setup() != null && (spec.hasCheck(ExtentSectionCheck.JSON_CONTRACT)
                || spec.hasCheck(ExtentSectionCheck.MOCK_RECONCILIATION))) {
            result = result.merge(fixture.reconcileMockContracts(page));
        }
        if (spec.hasCheck(ExtentSectionCheck.STYLE)) {
            result = result.merge(durationAuditor.inspect(page).validation());
        }
        SectionAuditCheckExecutor.Execution execution = checkExecutor.execute(section(), spec, result);
        return new SectionAuditResult(section(), spec.checks(), execution.executedChecks(),
                spec.journey(), execution.validation(),
                List.of("owners", "scenarios", "phases", "request evidence"));
    }
}

