package atf.reporting.audit;

import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class AssertionAuditStepComponentTest {
    @TempDir Path directory;
    @ParameterizedTest @ValueSource(booleans={true,false})
    void assertionGroupHasNoSyntheticHttpNodeAndCannotWeakenEvaluatedFailure(boolean extent) throws Exception {
        Path output=directory.resolve(extent?"extent.html":"fallback.html");
        try(AtfReportPorts.Session session=extent?new ExtentReportSession(output):new HtmlReportSession(output);
            var scenario=session.startScenario("AuditStep","independent business rows",List.of("component"));
            var phase=scenario.startPhase("TEST BODY")) {
            var row=new AtfAssertion("Independent expected","x".repeat(300)+"A","x".repeat(300)+"B",false);
            assertEquals(AtfReportStatus.FAIL,AssertionAuditReport.publishStep(phase,"Business checks",List.of(row),AtfReportStatus.PASS));
            phase.complete(AtfReportStatus.PASS);scenario.complete(AtfReportStatus.PASS);
            AtfReportSemanticSnapshot snapshot=extent?((ExtentReportSession)session).semanticSnapshot():((HtmlReportSession)session).semanticSnapshot();
            assertEquals(0,snapshot.requestCount());assertEquals(AtfReportStatus.FAIL,snapshot.classes().getFirst().scenarios().getFirst().status());
        }
        String html=Files.readString(output);
        assertTrue(html.contains("Business checks"));assertTrue(html.contains("assertion-step"));
        assertFalse(html.contains("ASSERT business-flow"));
    }
}
