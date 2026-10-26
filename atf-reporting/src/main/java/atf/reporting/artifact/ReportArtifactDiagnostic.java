package atf.reporting.artifact;

/**
 * Structured result for one published-report contract check.
 *
 * <p>The diagnostic keeps the contract owner separate from the check name so
 * callers can render, filter or export inspection results without parsing
 * human-readable text.</p>
 */
public record ReportArtifactDiagnostic(
        String owner,
        String check,
        boolean passed,
        String evidence,
        String message) {
    public ReportArtifactDiagnostic {
        owner = normalize(owner, "artifact");
        check = normalize(check, "unnamed check");
        evidence = normalize(evidence, "no evidence");
        message = normalize(message, passed ? "contract satisfied" : "contract violation");
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
