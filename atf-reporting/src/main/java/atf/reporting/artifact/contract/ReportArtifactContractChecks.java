package atf.reporting.artifact.contract;
import atf.reporting.artifact.ReportArtifactDiagnostic;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for the semantic contract checks of the published HTML report.
 *
 * <p>The facade deliberately owns only orchestration. Each contract group
 * owns one semantic surface and writes stable, human-readable check names to
 * the shared ordered result.</p>
 */
public final class ReportArtifactContractChecks {
    private record NamedGroup(String owner, ReportArtifactContractGroup evaluator) { }

    private static final List<NamedGroup> GROUPS = List.of(
            new NamedGroup("theme", ReportArtifactThemeContracts::evaluate),
            new NamedGroup("navigation", ReportArtifactNavigationContracts::evaluate),
            new NamedGroup("evidence", ReportArtifactEvidenceContracts::evaluate),
            new NamedGroup("overview", ReportArtifactOverviewContracts::evaluate),
            new NamedGroup("structure", ReportArtifactStructuralContracts::evaluate));

    private ReportArtifactContractChecks() { }

    public static Map<String, Boolean> evaluate(String html) {
        return evaluateDetailed(html).checks();
    }

    public static ReportArtifactContractEvaluation evaluateDetailed(String html) {
        var checks = new LinkedHashMap<String, Boolean>();
        var owners = new LinkedHashMap<String, String>();
        checks.put("html exists", true);
        owners.put("html exists", "artifact");
        checks.put("doctype", html.toLowerCase().contains("<!doctype html>"));
        owners.put("doctype", "structure");

        GROUPS.forEach(group -> {
            Set<String> before = Set.copyOf(checks.keySet());
            group.evaluator().evaluate(html, checks);
            checks.keySet().stream()
                    .filter(check -> !before.contains(check))
                    .forEach(check -> owners.put(check, group.owner()));
        });
        return new ReportArtifactContractEvaluation(checks, owners);
    }

    public static List<ReportArtifactDiagnostic> diagnostics(ReportArtifactContractEvaluation evaluation) {
        return evaluation.checks().entrySet().stream()
                .map(entry -> {
                    boolean passed = Boolean.TRUE.equals(entry.getValue());
                    String check = entry.getKey();
                    return new ReportArtifactDiagnostic(
                            evaluation.owners().getOrDefault(check, "artifact"),
                            check,
                            passed,
                            passed ? "required published marker matched" : "required published marker missing",
                            passed ? "Published artifact satisfies this contract"
                                    : "Published artifact does not satisfy this contract");
                })
                .toList();
    }
}
