package atf.reporting.ui.audit.section;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.audit.ReportAuditSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure Overview contract; it does not know Playwright or the HTML renderer. */
public final class OverviewSectionValidator {
    public ValidationResult validate(ReportAuditSnapshot.Overview overview,
                                      ReportAuditSnapshot.Tests tests,
                                      Expected expected) {
        Objects.requireNonNull(overview, "overview");
        Objects.requireNonNull(tests, "tests");
        Objects.requireNonNull(expected, "expected");
        List<AtfAssertion> assertions = new ArrayList<>();
        assertions.add(ReportAuditChecks.check("Overview sections",
                overview.requiredSectionCount() + "/" + overview.requiredSectionCount(),
                overview.sectionCount() + "/" + overview.requiredSectionCount(), overview.complete()));
        assertions.add(ReportAuditChecks.check("Test classes", String.valueOf(tests.classCount()),
                String.valueOf(overview.classCount()), overview.classCount() == tests.classCount()));
        assertions.add(ReportAuditChecks.check("Scenarios", String.valueOf(tests.scenarioCount()),
                String.valueOf(overview.scenarioCount()), overview.scenarioCount() == tests.scenarioCount()));
        assertions.add(ReportAuditChecks.check("Scenario audit total", String.valueOf(tests.scenarioCount()),
                String.valueOf(overview.auditScenarioTotal()), overview.auditScenarioTotal() == tests.scenarioCount()));
        if (!expected.configuredFamilies().isEmpty()) {
            assertions.add(ReportAuditChecks.check("Configured owners",
                    expected.expectedOwnerNames().toString(), tests.classNames().toString(),
                    tests.classNames().containsAll(expected.expectedOwnerNames())));
        }
        if (expected.mockRouteCount() > 0) {
            assertions.add(ReportAuditChecks.check("Mock-backed requests", "> 0",
                    String.valueOf(tests.requestCount()), tests.requestCount() > 0));
        }
        return ValidationResult.of(assertions);
    }

    public record Expected(List<String> configuredFamilies, int mockRouteCount,
                           Set<String> expectedOwnerNames) {
        public Expected {
            configuredFamilies = List.copyOf(Objects.requireNonNull(configuredFamilies, "configuredFamilies"));
            expectedOwnerNames = Set.copyOf(Objects.requireNonNull(expectedOwnerNames, "expectedOwnerNames"));
            if (mockRouteCount < 0) throw new IllegalArgumentException("mockRouteCount must be non-negative");
        }
    }
}
