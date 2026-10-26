package atf.ui.tests.extent.support.section.publish;

import atf.ui.tests.extent.support.section.module.ExtentSectionAuditModule;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.result.SectionAuditResult;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;

/** Renderer port for publishing a renderer-neutral section result. */
@FunctionalInterface
public interface SectionAuditPublisher {
    SectionAuditResult publish(SectionAuditFixture fixture, SectionAuditSpec spec,
                               ExtentSectionAuditModule module);
}
