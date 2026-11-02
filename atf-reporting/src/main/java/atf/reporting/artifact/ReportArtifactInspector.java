package atf.reporting.artifact;
import atf.reporting.artifact.contract.ReportArtifactContractChecks;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/**
 * Executable acceptance checks for the final HTML artifact.
 *
 * <p>This intentionally checks the published file, not the Extent builder.
 * A green unit test on a fragment is not evidence that the delivered report
 * is autonomous, navigable and structurally safe.</p>
 */
public final class ReportArtifactInspector {
    private ReportArtifactInspector() { }

    public static ReportArtifactInspection inspect(Path reportPath) {
        return inspectPublished(reportPath, ReportArtifactContractChecks::evaluateDetailed);
    }

    /** Inspects caller-declared intent; missing evidence never selects a permissive profile. */
    public static ProfileInspection inspect(Path reportPath, ReportArtifactProfile profile) {
        Objects.requireNonNull(profile, "profile");
        return new ProfileInspection(profile, inspectPublished(reportPath, html -> ReportProfileContractChecks.evaluate(html, profile)));
    }

    private static ReportArtifactInspection inspectPublished(Path reportPath,
            java.util.function.Function<String, atf.reporting.artifact.contract.ReportArtifactContractEvaluation> evaluator) {
        Objects.requireNonNull(reportPath, "reportPath");
        try {
            if (!Files.isRegularFile(reportPath)) {
                return ReportArtifactInspection.missing(reportPath);
            }
            String html = Files.readString(reportPath, StandardCharsets.UTF_8);
            var evaluation = evaluator.apply(html);
            return new ReportArtifactInspection(
                    reportPath,
                    evaluation.checks(),
                    ReportArtifactContractChecks.diagnostics(evaluation),
                    "");
        } catch (IOException error) {
            return new ReportArtifactInspection(reportPath, Map.of(), List.of(
                    new ReportArtifactDiagnostic("artifact", "html exists", false,
                            reportPath.toString(), error.toString())), error.toString());
        }
    }

    /**
     * Inspects the published artifact and verifies it was written after the
     * current execution began. This prevents an older HTML file from being
     * accepted when generation fails before publication.
     */
    public static ReportArtifactInspection inspectFresh(Path reportPath, Instant startedAt) {
        return fresh(reportPath, startedAt, inspect(reportPath));
    }

    /** Uses the same timestamp contract with an explicit evidence profile. */
    public static ProfileInspection inspectFresh(Path reportPath, Instant startedAt, ReportArtifactProfile profile) {
        var inspected = inspect(reportPath, profile);
        return new ProfileInspection(profile, fresh(reportPath, startedAt, inspected.artifact()));
    }

    private static ReportArtifactInspection fresh(Path reportPath, Instant startedAt, ReportArtifactInspection inspection) {
        Objects.requireNonNull(startedAt, "startedAt");
        if (!inspection.readError().isEmpty() || !Files.isRegularFile(reportPath)) {
            return inspection.withCheck("fresh artifact", false);
        }
        try {
            boolean fresh = Files.size(reportPath) > 0
                    && !Files.getLastModifiedTime(reportPath).toInstant().isBefore(startedAt);
            return inspection.withCheck("fresh artifact", fresh);
        } catch (IOException error) {
            return inspection.withCheck("fresh artifact", false, error.toString());
        }
    }

    /** Keeps the caller's complete intent attached to the result, including when the result is reconstructed. */
    public record ProfileInspection(ReportArtifactProfile profile, ReportArtifactInspection artifact) {
        public ProfileInspection { Objects.requireNonNull(profile, "profile"); Objects.requireNonNull(artifact, "artifact"); }
        public Path reportPath() { return artifact.reportPath(); }
        public Map<String, Boolean> checks() { return artifact.checks(); }
        public List<ReportArtifactDiagnostic> diagnostics() { return artifact.diagnostics(); }
        public String readError() { return artifact.readError(); }
        public boolean satisfies(String name) { return artifact.satisfies(name); }
        public boolean isCompliant() { return violations().isEmpty(); }
        public List<String> violations() {
            var result = new java.util.TreeSet<>(artifact.violations());
            var required = ReportProfileContractChecks.evaluate("", profile).checks().keySet();
            var allowed = new java.util.HashSet<>(required); allowed.add("fresh artifact");
            if (!checks().keySet().containsAll(required) || !allowed.containsAll(checks().keySet())) result.add("declared profile check inventory");
            return List.copyOf(result);
        }
    }

    public record ReportArtifactInspection(
            Path reportPath,
            Map<String, Boolean> checks,
            List<ReportArtifactDiagnostic> diagnostics,
            String readError) {
        public ReportArtifactInspection {
            Objects.requireNonNull(reportPath, "reportPath");
            checks.forEach((name, value) -> {
                if (name == null || name.isBlank()) throw new IllegalArgumentException("Inspection check name is required");
            });
            checks = Collections.unmodifiableMap(new LinkedHashMap<>(checks));
            diagnostics = List.copyOf(diagnostics);
            readError = readError == null ? "" : readError;
        }

        static ReportArtifactInspection missing(Path path) {
            return new ReportArtifactInspection(path, Map.of("html exists", false), List.of(
                    new ReportArtifactDiagnostic("artifact", "html exists", false,
                            path.toString(), "Report does not exist")), "Report does not exist");
        }

        public boolean isCompliant() {
            return violations().isEmpty();
        }

        public boolean satisfies(String name) {
            return Boolean.TRUE.equals(checks.get(name));
        }

        private ReportArtifactInspection withCheck(String name, boolean value) {
            return withCheck(name, value, readError);
        }

        private ReportArtifactInspection withCheck(String name, boolean value, String error) {
            var updated = new LinkedHashMap<>(checks);
            updated.put(name, value);
            var updatedDiagnostics = new java.util.ArrayList<>(diagnostics);
            updatedDiagnostics.removeIf(diagnostic -> diagnostic.check().equals(name));
            updatedDiagnostics.add(new ReportArtifactDiagnostic(
                    "artifact", name, value,
                    value ? "published artifact timestamp and size matched" : "published artifact freshness check failed",
                    error == null || error.isBlank()
                            ? (value ? "Artifact was published during this execution" : "Artifact was not published during this execution")
                            : error));
            return new ReportArtifactInspection(reportPath, updated, updatedDiagnostics, error);
        }

        public java.util.List<String> violations() {
            return ReportInspectionResultContracts.violations(checks, diagnostics, readError);
        }
    }
}
