package atf.reporting.audit;

import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.*;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Both adapters publish the same neutral facts without adding transport or validation counts. */
@AtfComponentTest
class ReportObservationComponentTest {
    @TempDir Path directory;
    @ParameterizedTest @ValueSource(booleans={true,false})
    void observationsRemainInspectableAndCannotUpgradeAFailedTest(boolean extent) throws Exception {
        Path output=directory.resolve(extent?"extent.html":"fallback.html");
        String reference="complete reference "+"word ".repeat(80)+"FINAL-TRANSCRIPT-ROW";
        String uri=directory.resolve("artifact-"+"deep-".repeat(30)).resolve("evidence.json").toUri().toASCIIString();
        var settings=new atf.reporting.configuration.ReportingSettings(240,12000);
        try (AtfReportPorts.Session session=extent?new ExtentReportSession(output,settings):new HtmlReportSession(output,settings);
             var scenario=session.startScenario("ObservationContract","native evidence",List.of("component"));
             var phase=scenario.startPhase("TEST BODY")) {
            assertTrue(phase.supportsObservations());
            phase.observation(new ReportObservation("Recognition evidence",List.of(new ReportField("Engine","whisper.cpp"),
                    new ReportField("Long scalar","x".repeat(300))),
                    List.of(new ReportObservation.Body("Complete transcript",reference,ReportObservation.Format.TEXT)),
                    List.of(new ReportObservation.Artifact("Run artifacts",java.net.URI.create(uri),"a".repeat(64)))));
            AssertionAuditReport.publishStep(phase,"Independent checks",List.of(new AtfAssertion("Original failure","expected","actual",false)),AtfReportStatus.PASS);
            phase.observation(new ReportObservation("After failure",List.of(new ReportField("Status fact","still failed")),List.of(),List.of()));
            phase.observation(new ReportObservation("Markup <b data-observation-probe='x'>title</b>",List.of(),List.of(),List.of()));
            phase.complete(AtfReportStatus.PASS); scenario.complete(AtfReportStatus.PASS);
            var snapshot=extent?((ExtentReportSession)session).semanticSnapshot():((HtmlReportSession)session).semanticSnapshot();
            assertEquals(0,snapshot.requestCount());assertEquals(0,snapshot.validationCount());
            var test=snapshot.classes().getFirst().scenarios().getFirst();
            assertEquals(AtfReportStatus.FAIL,test.status());
            var observed=(AtfReportSemanticSnapshot.ObservationSnapshot)test.phases().getFirst().nodes().getFirst();
            assertEquals(AtfReportStatus.INFO,observed.status());
            assertEquals(List.of("Engine","Long scalar"),observed.fieldNames());
            assertFalse(observed.toString().contains(reference));
            assertFalse(observed.toString().contains(uri));
            assertThrows(IllegalStateException.class,()->phase.observation(new ReportObservation("closed",List.of(),List.of(),List.of())));
        }
        String html=Files.readString(output);
        assertTrue(html.contains("FINAL-TRANSCRIPT-ROW"));assertTrue(html.contains("atf-observation-evidence"));
        assertTrue(html.contains("x".repeat(239)+"…"));assertFalse(html.contains("x".repeat(240)));
        assertTrue(html.contains(uri.replace("&","&amp;")));
        assertFalse(html.contains("<b data-observation-probe='x'>"));
    }
}
