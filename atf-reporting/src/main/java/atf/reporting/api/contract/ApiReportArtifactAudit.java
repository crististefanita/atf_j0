package atf.reporting.api.contract;

import atf.reporting.artifact.ReportArtifactInspector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composition root for a static API report audit.
 *
 * <p>The structural artifact contract and the catalog-backed JSON evidence
 * contract are deliberately kept as separate validators. This facade only
 * combines their results so a reporting consumer can verify one published
 * HTML file without importing Playwright, YAML loading or a concrete mock
 * transport.</p>
 */
public final class ApiReportArtifactAudit {
    private final ApiReportJsonEvidenceAuditor jsonEvidenceAuditor;

    public ApiReportArtifactAudit(ApiReportContractCatalog catalog) {
        jsonEvidenceAuditor = new ApiReportJsonEvidenceAuditor(
                Objects.requireNonNull(catalog, "catalog"));
    }

    public Result inspect(Path report) {
        Objects.requireNonNull(report, "report");
        ReportArtifactInspector.ReportArtifactInspection artifact =
                ReportArtifactInspector.inspect(report);
        if (!Files.isRegularFile(report) || !artifact.readError().isEmpty()) {
            String reason = !Files.isRegularFile(report)
                    ? "API report artifact does not exist: " + report
                    : artifact.readError();
            return new Result(report, artifact,
                    new ApiReportJsonEvidenceAuditor.Result(0, 0, List.of(reason)));
        }
        return new Result(report, artifact, jsonEvidenceAuditor.inspect(report));
    }

    public record Result(Path report,
                         ReportArtifactInspector.ReportArtifactInspection artifact,
                         ApiReportJsonEvidenceAuditor.Result jsonEvidence) {
        public Result {
            report = Objects.requireNonNull(report, "report");
            artifact = Objects.requireNonNull(artifact, "artifact");
            jsonEvidence = Objects.requireNonNull(jsonEvidence, "jsonEvidence");
        }

        public boolean compliant() {
            return artifact.isCompliant() && jsonEvidence.compliant();
        }

        /** Returns stable, namespaced violations suitable for a report step. */
        public List<String> violations() {
            List<String> result = new ArrayList<>();
            artifact.violations().forEach(violation -> result.add("artifact: " + violation));
            jsonEvidence.violations().forEach(violation -> result.add("json: " + violation));
            return List.copyOf(result);
        }
    }
}
