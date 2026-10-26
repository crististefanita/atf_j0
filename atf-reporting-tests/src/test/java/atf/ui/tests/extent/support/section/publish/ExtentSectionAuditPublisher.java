package atf.ui.tests.extent.support.section.publish;
import atf.reporting.audit.ReportAuditChecks;

import atf.ui.tests.extent.support.section.module.ExtentSectionAuditModule;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import com.microsoft.playwright.Page;

import java.util.Objects;

/**
 * Extent/JUnit adapter for a section module.
 *
 * <p>The module owns extraction and pure validation only. This adapter owns
 * scenario/phase/evidence publication and converts the typed result into the
 * existing Extent lifecycle, leaving the module independent of renderers.</p>
 */
public final class ExtentSectionAuditPublisher implements SectionAuditPublisher {
    @Override
    public SectionAuditResult publish(SectionAuditFixture fixture, SectionAuditSpec spec,
                                      ExtentSectionAuditModule module) {
        Objects.requireNonNull(fixture, "fixture");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(module, "module");
        return fixture.audit().auditResult(module.auditTitle(), audit -> {
            SectionAuditResult result = module.audit(fixture, spec);
            Page evidencePage = module.evidencePage(fixture);
            audit.screen(module.evidenceTitle(), module.evidenceTarget(),
                    fixture.audit().capture(evidencePage, module.evidenceTitle()),
                    result.assertions(), ReportAuditChecks.status(result.valid()));
            if (!result.valid()) throw new AssertionError(result.failureSummary());
            return result;
        });
    }
}
