package atf.reporting.artifact;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportArtifactIdentityTest {
    @Test
    void displayIsReadableDynamicFileLabelWhileDigestRemainsTechnicalMetadata() throws Exception {
        var report = Files.createTempFile("atf-report-", ".html");
        try {
            Files.writeString(report, "<!doctype html><html></html>");
            ReportArtifactIdentity identity = ReportArtifactIdentity.read(report);

            assertEquals(report.getFileName().toString(), identity.display());
            assertFalse(identity.display().contains("SHA-256"));
            assertTrue(identity.sha256().matches("[A-F0-9]{64}"));
        } finally {
            Files.deleteIfExists(report);
        }
    }
}
