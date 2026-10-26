package atf.reporting.ui.audit.result;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ValidationResult;

import atf.reporting.ui.audit.plan.ExtentJourney;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Renderer-neutral outcome of one section audit.
 *
 * <p>The section module may use Playwright to extract facts and may publish
 * evidence through an adapter, but the outcome itself contains no browser,
 * Extent or JUnit object. This keeps the same verdict reusable by unit tests,
 * Extent publishers and other report renderers.</p>
 */
public record SectionAuditResult(ExtentSection section,
                                 Set<ExtentSectionCheck> requestedChecks,
                                 Set<ExtentSectionCheck> executedChecks,
                                 ExtentJourney journey,
                                 ValidationResult validation,
                                 List<String> evidence) {
    public SectionAuditResult {
        section = Objects.requireNonNull(section, "section");
        requestedChecks = Set.copyOf(Objects.requireNonNull(requestedChecks, "requestedChecks"));
        executedChecks = Set.copyOf(Objects.requireNonNull(executedChecks, "executedChecks"));
        journey = Objects.requireNonNull(journey, "journey");
        validation = Objects.requireNonNull(validation, "validation");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (!requestedChecks.containsAll(executedChecks)) {
            throw new IllegalArgumentException("executed checks must be declared by the audit spec");
        }
    }

    public boolean valid() {
        return validation.valid();
    }

    public List<AtfAssertion> assertions() {
        return validation.assertions();
    }

    public List<String> violations() {
        return validation.violations();
    }

    public String failureSummary() {
        return validation.failureSummary();
    }
}
