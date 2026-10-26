package atf.ui.tests.extent.support.section.module;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.runtime.SectionAuditCheckExecutor;
import atf.reporting.audit.ValidationResult;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.reporting.ui.audit.section.UiValidationSectionValidator;


/** Screen-validation browser adapter for the compact three-zone evidence layout. */
public final class UiValidationSectionAuditModule implements ExtentSectionAuditModule {
    private final UiValidationSectionValidator validator = new UiValidationSectionValidator();
    private final SectionAuditCheckExecutor checkExecutor = new SectionAuditCheckExecutor();

    @Override public ExtentSection section() { return ExtentSection.UI_VALIDATION; }

    @Override
    public SectionAuditResult audit(SectionAuditFixture fixture, SectionAuditSpec spec) {
        var page = fixture.flow().openTests();
        var owners = fixture.flow().ownerNames(page);
        ReportAuditSnapshot.UiValidation snapshot = null;
        for (String ownerName : owners) {
            var owner = fixture.flow().openOwner(page, ownerName);
            fixture.flow().expandAll(owner.detail());
            ReportAuditSnapshot.UiValidation candidate = fixture.exploration()
                    .inspectUiValidation(page);
            if (snapshot == null || candidate.screenshotLinkCount() > snapshot.screenshotLinkCount()) {
                snapshot = candidate;
            }
            if (candidate.inlineScreenshot() && candidate.actionSequenceCount() > 0
                    && candidate.observationCount() > 0 && candidate.assertionTableCount() > 0) {
                break;
            }
        }
        if (snapshot == null) {
            snapshot = new ReportAuditSnapshot.UiValidation(0, 0, 0, 0, 0,
                    0, 0, false, false, false, false, false,
                    java.util.List.of("no API owner is rendered in the Tests tab"));
        }
        ValidationResult result = (spec.hasCheck(ExtentSectionCheck.STRUCTURE)
                || spec.hasCheck(ExtentSectionCheck.SCREENSHOT_LINK)
                || spec.hasCheck(ExtentSectionCheck.INLINE_ALIGNMENT))
                ? validator.validate(snapshot) : ValidationResult.empty();
        SectionAuditCheckExecutor.Execution execution = checkExecutor.execute(section(), spec, result);
        return new SectionAuditResult(section(), spec.checks(), execution.executedChecks(),
                spec.journey(), execution.validation(),
                java.util.List.of("action sequence", "audit observations", "assertions", "inline screenshot"));
    }
}
