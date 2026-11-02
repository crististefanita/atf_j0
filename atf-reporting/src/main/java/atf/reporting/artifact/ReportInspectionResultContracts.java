package atf.reporting.artifact;

import atf.reporting.artifact.contract.ReportArtifactContractChecks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Result consistency is distinct from file inspection; constructors do not assert that a file was inspected. */
final class ReportInspectionResultContracts {
    private static final Set<String> LEGACY = Set.copyOf(ReportArtifactContractChecks.evaluate("").keySet());
    private ReportInspectionResultContracts() { }

    static List<String> violations(Map<String, Boolean> checks, List<ReportArtifactDiagnostic> diagnostics, String error) {
        var result = new HashSet<String>();
        var required = checks.containsKey("profile renderer") ? ReportProfileContractChecks.REQUIRED : LEGACY;
        if (!checks.keySet().containsAll(required)) result.add("inspection check inventory");
        checks.forEach((name, passed) -> { if (!Boolean.TRUE.equals(passed)) result.add(name); });
        var seen = new HashSet<String>();
        for (var diagnostic : diagnostics) {
            if (diagnostic.owner() == null || diagnostic.owner().isBlank() || !seen.add(diagnostic.check())
                    || !checks.containsKey(diagnostic.check()) || !java.util.Objects.equals(checks.get(diagnostic.check()), diagnostic.passed()))
                result.add("inspection diagnostics");
        }
        if (!seen.equals(checks.keySet())) result.add("inspection diagnostics");
        if (!error.isEmpty()) result.add("artifact read error");
        var ordered = new ArrayList<>(result); ordered.sort(String::compareTo); return List.copyOf(ordered);
    }
}
