package atf.ui.tests.extent.support.section.runtime;

import atf.testkit.setup.api.ApiScenarioSetup;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.plan.UiExtentReportSource;

import java.util.Objects;

/** Test-context preconditions that must stay outside the reporting module. */
public final class SectionAuditPreconditions {
    private SectionAuditPreconditions() {
    }

    public static SectionAuditSpec resolve(Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        SectionAuditSpec spec = SectionAuditSpec.resolve(owner);
        if (spec.source() == UiExtentReportSource.GENERATED_API
                && owner.getAnnotation(ApiScenarioSetup.class) == null) {
            throw new IllegalArgumentException(owner.getName()
                    + " uses GENERATED_API but has no @ApiScenarioSetup");
        }
        return spec;
    }
}
