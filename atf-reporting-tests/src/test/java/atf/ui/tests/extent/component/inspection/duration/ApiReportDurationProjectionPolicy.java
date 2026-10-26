package atf.ui.tests.extent.component.inspection.duration;
import atf.reporting.audit.ValidationResult;

import atf.reporting.audit.ReportAuditSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Renderer-neutral policy for the four duration projections. */
public final class ApiReportDurationProjectionPolicy {
    private static final Set<String> EXPECTED = Set.of("Tests", "Overview", "Categories", "Fails");
    private static final Set<String> ALLOW_EMPTY = Set.of("Categories", "Fails");

    public ValidationResult validate(List<ReportAuditSnapshot.DurationProjection> projections) {
        List<String> violations = new ArrayList<>();
        Set<String> actual = projections.stream()
                .map(ReportAuditSnapshot.DurationProjection::projection)
                .collect(Collectors.toSet());
        if (!actual.equals(EXPECTED)) {
            violations.add("duration projections expected " + EXPECTED + " but were " + actual);
        }
        projections.forEach(projection -> {
            boolean valid = projection.valid()
                    && (projection.fieldCount() > 0 || ALLOW_EMPTY.contains(projection.projection()));
            if (!valid) violations.add(projection.projection() + " duration projection is invalid: " + projection.violations());
        });
        return ValidationResult.ofViolations(violations);
    }

    public Set<String> expectedProjections() {
        return EXPECTED;
    }
}
