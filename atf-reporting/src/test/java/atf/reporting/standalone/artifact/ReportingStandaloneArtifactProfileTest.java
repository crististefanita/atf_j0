package atf.reporting.standalone.artifact;

import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.contract.AtfReportStatus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportingStandaloneArtifactProfileTest {
    @Test void anEmptyInventoryCannotDefineAProfile() {
        assertThrows(IllegalArgumentException.class, () -> new ReportArtifactProfile(ReportArtifactProfile.Renderer.FALLBACK, List.of()));
    }
    @Test void aPhaseCannotConcealAFailedComparison() {
        var failed = new ReportArtifactProfile.Assertions("Original rows", List.of(new ReportArtifactProfile.Comparison("last row", false)));
        assertThrows(IllegalArgumentException.class, () -> new ReportArtifactProfile.Phase("TEST BODY", AtfReportStatus.PASS, List.of(failed)));
    }
    @Test void aScenarioCannotConcealAFailedPhase() {
        var phase = new ReportArtifactProfile.Phase("TEST BODY", AtfReportStatus.FAIL, List.of());
        assertThrows(IllegalArgumentException.class, () -> new ReportArtifactProfile.Scenario("Fixture", "original", AtfReportStatus.PASS, List.of(phase)));
    }
    @Test void advisoryAndSkippedVerdictsRemainExplicit() {
        for (var status : List.of(AtfReportStatus.WARNING, AtfReportStatus.SKIP, AtfReportStatus.INFO)) {
            var group = new ReportArtifactProfile.Assertions("Original rows", List.of(new ReportArtifactProfile.Comparison("explicit outcome", status)));
            assertEquals(status, group.status());
        }
    }
    @Test void callerCollectionsCannotAlterAnAcceptedIntent() {
        var rows = new ArrayList<>(List.of(new ReportArtifactProfile.Comparison("independent row", true)));
        var group = new ReportArtifactProfile.Assertions("Original rows", rows);
        rows.clear(); assertEquals(1, group.comparisons().size());
        assertThrows(UnsupportedOperationException.class, () -> group.comparisons().clear());
    }
}
