package atf.reporting.distribution;

import atf.testkit.AtfIntegrationTest;
import atf.reporting.extent.session.ExtentReportSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfIntegrationTest
class ReportingLibraryDistributionIntegrationTest {
    @TempDir Path directory;
    @Test void isolatedSourceAndAnExternalMavenConsumerProveThePhysicalLibraryBoundary() throws Exception {
        var result=ReportingDistributionProbe.verify(directory);
        boolean sourceDeployment=Files.isDirectory(Path.of(ExtentReportSession.class
                .getProtectionDomain().getCodeSource().getLocation().toURI()));
        assertEquals(sourceDeployment,result.sourceBuild(),"The gate must exercise the actual loaded deployment");
        if(sourceDeployment) assertTrue(result.isolatedBuild().contains("BUILD SUCCESS"));
        else assertEquals("",result.isolatedBuild(),"A binary consumer never rebuilds a neighbouring producer");
        assertEquals(0,result.nominal().exit(),result.nominal()::text);
        assertTrue(result.nominal().text().contains("RENDERER=EXTENT; statuses=PASS,FAIL,PASS"));
        assertTrue(result.nominal().text().contains("RENDERER=FALLBACK; statuses=PASS,FAIL,PASS"));
        assertTrue(result.nominal().text().contains("SHA256="+result.run().packageSha256()));
        assertTrue(result.nominal().text().contains("RUN_ID="+result.run().id()));
        assertTrue(result.nominal().text().contains("forbidden-ATF-runner-browser-packages=ABSENT"));
        for(var refusal:java.util.List.of(result.extentRefusal(),result.fallbackRefusal())) {
            assertNotEquals(0,refusal.exit());
            assertTrue(refusal.text().contains("Missing reporting asset: /atf/reporting/css/table-viewport.css"),refusal::text);
            assertTrue(refusal.text().contains("WORKSPACE_ROLLBACK=PASS; previous-artifact=PRESERVED"),refusal::text);
        }
        Path relocated = Files.move(result.reports(), directory.resolve("relocated-reports"));
        assertFalse(Files.exists(result.reports()));
        for (String name : List.of("extent.html.receipt", "fallback.html.receipt")) {
            var verification = ReportEvidenceReceipt.verify(relocated.resolve(name), result.run());
            assertEquals(ReportEvidenceReceipt.Status.VERIFIED, verification.status(), name);
            assertEquals(ReportEvidenceReceipt.Verdict.PASS, verification.historicalVerdict());
        }
    }
}
