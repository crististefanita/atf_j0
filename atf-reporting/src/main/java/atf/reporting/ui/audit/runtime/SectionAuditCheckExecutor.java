package atf.reporting.ui.audit.runtime;
import atf.reporting.audit.ValidationResult;

import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.plan.SectionAuditSpec;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Executes the declarative check plan for one section.
 *
 * <p>Modules collect browser observations and run their pure validator. This
 * class is the only owner of check projection and executed-check accounting;
 * a module therefore cannot silently report a different check set or forget
 * to apply the selected policy.</p>
 */
public final class SectionAuditCheckExecutor {
    public Execution execute(ExtentSection section, SectionAuditSpec spec,
                             ValidationResult collected) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(collected, "collected");
        Set<ExtentSectionCheck> applicable = SectionAuditCheckPolicy.applicableChecks(
                section, spec.checks());
        ValidationResult projected = SectionAuditCheckPolicy.select(
                section, applicable, collected);
        return new Execution(applicable, projected);
    }

    public record Execution(Set<ExtentSectionCheck> executedChecks,
                            ValidationResult validation) {
        public Execution {
            executedChecks = executedChecks.isEmpty()
                    ? Set.of()
                    : Set.copyOf(EnumSet.copyOf(executedChecks));
            validation = Objects.requireNonNull(validation, "validation");
        }
    }
}
