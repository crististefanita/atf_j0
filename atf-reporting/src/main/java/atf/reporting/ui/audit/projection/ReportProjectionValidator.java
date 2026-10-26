package atf.reporting.ui.audit.projection;

import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.audit.ValidationResult;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.ui.audit.plan.ReportProjection;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/** Pure comparison between a report projection and an observed Tests snapshot. */
public final class ReportProjectionValidator {
    public ValidationResult validate(ReportProjection expected,
                                     ReportAuditSnapshot.Tests observed) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(observed, "observed");
        boolean matches = expected.matches(observed);
        List<String> expectedOwners = expected.owners().stream()
                .map(ReportProjection.Owner::name).toList();
        List<AtfAssertion> assertions = new ArrayList<>(List.of(
                ReportAuditChecks.check("Expected owners", expectedOwners.toString(),
                        observed.classNames().toString(), sameMultiset(observed.classNames(), expectedOwners)),
                ReportAuditChecks.check("Selected scenario count", String.valueOf(expected.scenarioCount()),
                        String.valueOf(observed.scenarioCount()), observed.scenarioCount() == expected.scenarioCount()),
                ReportAuditChecks.check("Selected scenario titles", expected.scenarioTitles().toString(),
                        observed.scenarioTitles().toString(),
                        sameMultiset(observed.scenarioTitles(), expected.scenarioTitles())),
                ReportAuditChecks.check("Projected phases", ">= " + expected.phaseCount(),
                        String.valueOf(observed.phaseCount()), observed.phaseCount() >= expected.phaseCount()),
                ReportAuditChecks.check("Projected requests", ">= " + expected.requestCount(),
                        String.valueOf(observed.requestCount()), observed.requestCount() >= expected.requestCount()),
                ReportAuditChecks.check("Projected validations", ">= " + expected.requestCount(),
                        String.valueOf(observed.validationCount()), observed.validationCount() >= expected.requestCount()),
                ReportAuditChecks.check("ReportProjection", "owners + scenarios + counts match",
                        String.valueOf(matches), matches)));
        if (expected.hasStatusExpectation()) {
            assertions.add(ReportAuditChecks.check("Scenario statuses",
                    expected.expectedScenarioStatuses().toString(),
                    observed.scenarioStatuses().toString(),
                    expected.expectedScenarioStatuses().equals(observed.scenarioStatuses())));
        }
        return ValidationResult.of(assertions);
    }

    private static boolean sameMultiset(List<String> actual, List<String> expected) {
        return actual.size() == expected.size() && actual.stream().sorted().toList()
                .equals(expected.stream().sorted().toList());
    }
}
