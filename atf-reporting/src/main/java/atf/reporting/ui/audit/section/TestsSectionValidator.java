package atf.reporting.ui.audit.section;

import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.audit.ValidationResult;
import atf.reporting.ui.audit.plan.ReportProjection;
import atf.reporting.ui.audit.projection.ReportProjectionValidator;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure validator for the Tests projection; no Page, Locator or JUnit dependency. */
public final class TestsSectionValidator {
    private final ReportProjectionValidator projectionValidator = new ReportProjectionValidator();

    public ValidationResult validateProjection(ReportProjection projection,
                                               ReportAuditSnapshot.Tests snapshot) {
        return projectionValidator.validate(projection, snapshot);
    }

    public ValidationResult validate(ReportAuditSnapshot.Tests snapshot,
                                     Collection<String> expectedOwners) {
        Objects.requireNonNull(snapshot, "snapshot");
        Set<String> expected = new LinkedHashSet<>(Objects.requireNonNull(expectedOwners, "expectedOwners"));
        Set<String> actual = new LinkedHashSet<>(snapshot.classNames());
        boolean ownersValid = expected.isEmpty() ? !actual.isEmpty() : actual.equals(expected);
        return ValidationResult.of(List.of(
                ReportAuditChecks.check("Owners", expected.isEmpty() ? "> 0" : expected.toString(),
                        actual.toString(), ownersValid),
                ReportAuditChecks.check("Scenarios", "> 0", String.valueOf(snapshot.scenarioCount()),
                        snapshot.scenarioCount() > 0),
                ReportAuditChecks.check("Phases", ">= scenarios", String.valueOf(snapshot.phaseCount()),
                        snapshot.phaseCount() >= snapshot.scenarioCount()),
                ReportAuditChecks.check("Requests", "> 0", String.valueOf(snapshot.requestCount()),
                        snapshot.requestCount() > 0),
                ReportAuditChecks.check("Validation cards", "> 0", String.valueOf(snapshot.validationCount()),
                        snapshot.validationCount() > 0)));
    }

    public ValidationResult validate(ReportAuditSnapshot.Tests snapshot,
                                     Collection<String> expectedOwners,
                                     ReportProjection projection) {
        return validate(snapshot, expectedOwners).merge(projectionValidator.validate(projection, snapshot));
    }
}
