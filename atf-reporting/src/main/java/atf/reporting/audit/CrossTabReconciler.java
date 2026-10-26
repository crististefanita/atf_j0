package atf.reporting.audit;
import atf.reporting.contract.AtfAssertion;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Reconciles the semantic population projected by the report tabs. */
public final class CrossTabReconciler {
    public ValidationResult reconcile(Collection<String> expectedOwners,
                                      Collection<String> actualOwners,
                                      int overviewClassCount,
                                      int linkedCategoryScenarioCount) {
        Set<String> expected = normalized(expectedOwners);
        Set<String> actual = normalized(actualOwners);
        List<AtfAssertion> assertions = List.of(
                ReportAuditChecks.check("Owners", expected.toString(), actual.toString(),
                        actual.equals(expected)),
                ReportAuditChecks.check("Overview classes", String.valueOf(actual.size()),
                        String.valueOf(overviewClassCount), overviewClassCount == actual.size()),
                ReportAuditChecks.check("Category links", "> 0",
                        String.valueOf(linkedCategoryScenarioCount), linkedCategoryScenarioCount > 0));
        return ValidationResult.of(assertions);
    }

    private static Set<String> normalized(Collection<String> owners) {
        Objects.requireNonNull(owners, "owners");
        return owners.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
