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
        Objects.requireNonNull(reportPath, "reportPath");
        try {
            if (!Files.isRegularFile(reportPath)) {
                return ReportArtifactInspection.missing(reportPath);
            }
            String html = Files.readString(reportPath, StandardCharsets.UTF_8);
            var evaluation = ReportArtifactContractChecks.evaluateDetailed(html);
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
        Objects.requireNonNull(startedAt, "startedAt");
        var inspection = inspect(reportPath);
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

    public record ReportArtifactInspection(
            Path reportPath,
            Map<String, Boolean> checks,
            List<ReportArtifactDiagnostic> diagnostics,
            String readError) {
        public ReportArtifactInspection {
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
            return readError.isEmpty() && checks.values().stream().allMatch(Boolean::booleanValue);
        }

        public boolean satisfies(String name) {
            return checks.getOrDefault(name, false);
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
            return checks.entrySet().stream().filter(entry -> !entry.getValue())
                    .map(Map.Entry::getKey).sorted().toList();
        }
    }
}
