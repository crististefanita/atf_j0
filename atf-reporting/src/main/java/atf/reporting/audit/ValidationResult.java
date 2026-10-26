package atf.reporting.audit;
import atf.reporting.contract.AtfAssertion;

import java.util.List;
import java.util.Objects;
import java.util.Collection;
import java.util.ArrayList;

/** Immutable result shared by report validators and cross-tab reconcilers. */
public record ValidationResult(boolean valid, List<AtfAssertion> assertions,
                               List<String> violations) {
    public ValidationResult {
        assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions"));
        violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
    }

    public static ValidationResult of(List<AtfAssertion> assertions) {
        Objects.requireNonNull(assertions, "assertions");
        List<String> violations = assertions.stream()
                .filter(assertion -> !assertion.passed())
                .map(assertion -> assertion.name() + ": expected " + assertion.expected()
                        + ", actual " + assertion.actual())
                .toList();
        return new ValidationResult(violations.isEmpty(), assertions, violations);
    }

    public static ValidationResult empty() {
        return new ValidationResult(true, List.of(), List.of());
    }

    /** Adapts a legacy finding list without losing the renderer-neutral contract. */
    public static ValidationResult fromViolations(String owner, Collection<String> findings) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(findings, "findings");
        List<AtfAssertion> assertions = new ArrayList<>();
        int index = 1;
        for (String finding : findings) {
            String actual = finding == null || finding.isBlank() ? "<blank finding>" : finding;
            assertions.add(new AtfAssertion(owner + " finding " + index++, "no violation", actual, false));
        }
        return new ValidationResult(findings.isEmpty(), assertions, findings.stream()
                .map(value -> value == null ? "<blank finding>" : value).toList());
    }

    /** Compatibility adapter for pure validators that do not need an owner label. */
    public static ValidationResult ofViolations(Collection<String> violations) {
        Objects.requireNonNull(violations, "violations");
        List<String> copy = violations.stream().filter(Objects::nonNull).toList();
        return new ValidationResult(copy.isEmpty(), List.of(), copy);
    }

    /** Merges heterogeneous auditor findings into one stable report/JUnit result. */
    public ValidationResult merge(ValidationResult other) {
        Objects.requireNonNull(other, "other");
        List<AtfAssertion> mergedAssertions = new ArrayList<>(assertions);
        mergedAssertions.addAll(other.assertions());
        List<String> mergedViolations = new ArrayList<>(violations);
        mergedViolations.addAll(other.violations());
        return new ValidationResult(mergedViolations.isEmpty(), mergedAssertions, mergedViolations);
    }

    public static ValidationResult merge(Collection<ValidationResult> results) {
        Objects.requireNonNull(results, "results");
        ValidationResult merged = new ValidationResult(true, List.of(), List.of());
        for (ValidationResult result : results) merged = merged.merge(result);
        return merged;
    }

    /** Convenience overload for small, explicit convergence checks. */
    public static ValidationResult merge(ValidationResult... results) {
        Objects.requireNonNull(results, "results");
        return merge(List.of(results));
    }

    public String failureSummary() {
        return violations.isEmpty() ? "" : String.join("; ", violations);
    }
}
