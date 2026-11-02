package atf.reporting.standalone.artifact;

import atf.reporting.artifact.ReportArtifactDiagnostic;
import atf.reporting.artifact.ReportArtifactInspector.ReportArtifactInspection;
import atf.reporting.artifact.contract.ReportArtifactContractChecks;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** A constructed result cannot certify absent, partial or contradictory inspection evidence. */
class ReportingStandaloneInspectionResultTest {
    private static final Path FILE = Path.of("authored-inspection.html");

    @Test void emptyEvidenceCannotPass() {
        refuse(new ReportArtifactInspection(FILE, Map.of(), List.of(), ""));
    }

    @Test void onePassingCheckCannotReplaceTheDeclaredLegacyInventory() {
        refuse(new ReportArtifactInspection(FILE, Map.of("doctype", true), List.of(diagnostic("doctype", true)), ""));
    }

    @Test void contradictoryDiagnosticCannotPass() {
        var checks = completeChecks();
        var diagnostics = new java.util.ArrayList<>(diagnostics(checks));
        diagnostics.set(0, diagnostic(diagnostics.getFirst().check(), false));
        refuse(new ReportArtifactInspection(FILE, checks, diagnostics, ""));
    }

    @Test void absentDiagnosticCannotPass() {
        var checks = completeChecks();
        refuse(new ReportArtifactInspection(FILE, checks, diagnostics(checks).subList(1, checks.size()), ""));
    }

    @Test void duplicateDiagnosticCannotPass() {
        var checks = completeChecks();
        var diagnostics = new java.util.ArrayList<>(diagnostics(checks));
        diagnostics.add(diagnostics.getFirst());
        refuse(new ReportArtifactInspection(FILE, checks, diagnostics, ""));
    }

    @Test void completeConsistentResultKeepsCallerCollectionsImmutable() {
        var checks = completeChecks();
        var diagnostics = new java.util.ArrayList<>(diagnostics(checks));
        var result = new ReportArtifactInspection(FILE, checks, diagnostics, "");
        checks.clear(); diagnostics.clear();
        assertTrue(result.isCompliant());
        assertTrue(result.violations().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.checks().clear());
    }

    private static void refuse(ReportArtifactInspection result) {
        assertFalse(result.isCompliant());
        assertFalse(result.violations().isEmpty(), "noncompliance needs an actionable violation");
    }
    private static Map<String, Boolean> completeChecks() {
        var result = new LinkedHashMap<String, Boolean>();
        ReportArtifactContractChecks.evaluate("").keySet().forEach(name -> result.put(name, true));
        return result;
    }
    private static List<ReportArtifactDiagnostic> diagnostics(Map<String, Boolean> checks) {
        return checks.keySet().stream().map(name -> diagnostic(name, true)).toList();
    }
    private static ReportArtifactDiagnostic diagnostic(String check, boolean passed) {
        return new ReportArtifactDiagnostic("authored-result", check, passed, "authored evidence", "independent result consistency fixture");
    }
}
