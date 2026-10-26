package atf.reporting.ui.audit.section;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.audit.ReportAuditSnapshot;

import java.util.List;
import java.util.Objects;

/** Pure contract for Categories; cardinality remains data-driven. */
public final class CategoriesSectionValidator {
    public ValidationResult validate(ReportAuditSnapshot.Annotations snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean cardinalities = snapshot.hasConsistentLinkCardinality()
                && snapshot.linkCardinalities().stream().allMatch(value -> value >= 0);
        boolean canonicalSurface = isCanonicalHexSurface(snapshot.filterSurface());
        return ValidationResult.of(List.of(
                ReportAuditChecks.check("Categories", "> 0", String.valueOf(snapshot.categoryCount()),
                        snapshot.categoryCount() > 0),
                ReportAuditChecks.check("Linked scenarios", "> 0", String.valueOf(snapshot.linkedScenarioCount()),
                        snapshot.linkedScenarioCount() > 0),
                ReportAuditChecks.check("Category surface", "styled", snapshot.filterSurface(),
                        snapshot.styled() && canonicalSurface),
                ReportAuditChecks.check("Link cardinality", "dynamic zero/one/many",
                        snapshot.linkCardinalities().toString(), cardinalities)));
    }

    private static boolean isCanonicalHexSurface(String surface) {
        return surface != null
                && surface.matches("(?i)#[0-9a-f]{3,8}\\s*/\\s*#[0-9a-f]{3,8}");
    }
}
