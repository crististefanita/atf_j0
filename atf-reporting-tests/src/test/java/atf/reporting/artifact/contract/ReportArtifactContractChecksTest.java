package atf.reporting.artifact.contract;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportArtifactContractChecksTest {
    @Test
    void keepsArtifactContractChecksPureAndNamed() {
        var checks = ReportArtifactContractChecks.evaluate("<!doctype html><html></html>");

        assertTrue(checks.get("html exists"));
        assertTrue(checks.get("doctype"));
        assertFalse(checks.get("ATF dark theme"));
        assertTrue(checks.containsKey("overview order"));
        assertTrue(checks.containsKey("result markers"));
    }

    @Test
    void rejectsMissingStructuralEvidenceWithoutThrowing() {
        var checks = ReportArtifactContractChecks.evaluate("<html><body>incomplete</body></html>");

        assertTrue(checks.get("html exists"));
        assertFalse(checks.get("doctype"));
        assertFalse(checks.get("request-validation roles"));
        assertFalse(checks.get("aggregate assertion table"));
    }

    @Test
    void exposesStableOwnersAndActionableDiagnosticEvidence() {
        var evaluation = ReportArtifactContractChecks.evaluateDetailed("<!doctype html><html></html>");
        var diagnostics = ReportArtifactContractChecks.diagnostics(evaluation);

        assertTrue(diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.owner().equals("theme") && diagnostic.check().equals("ATF dark theme")));
        assertTrue(diagnostics.stream().allMatch(diagnostic ->
                !diagnostic.owner().isBlank()
                        && !diagnostic.evidence().isBlank()
                        && !diagnostic.message().isBlank()));
        assertTrue(diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.check().equals("ATF dark theme") && !diagnostic.passed()
                        && diagnostic.message().contains("does not satisfy")));
    }
}
