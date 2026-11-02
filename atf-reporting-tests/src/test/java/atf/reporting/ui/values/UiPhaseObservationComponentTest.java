package atf.reporting.ui.values;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.artifact.*;
import atf.reporting.contract.*;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.*;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiPhaseObservationComponentTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void observationsStayAtTheirPhaseWithoutCreatingAssertionsOrRequests(boolean extent) throws Exception {
        Path path = TestReportPaths.report("ui-phase-observation-" + extent + ".html");
        try (AtfReportPorts.Session session = extent ? new ExtentReportSession(path, new ReportingSettings(240, 0))
                : new HtmlReportSession(path, new ReportingSettings(240, 0))) {
            var scenario = UiExtentReportSession.adaptScenario(session.startScenario("NeutralFacts", "phase ownership", List.of("component")),
                    ReportEvidenceProfile.defaults(), "phase ownership");
            for (String name : List.of("SETUP", "TEST BODY", "TEARDOWN")) {
                try (var phase = scenario.startPhase(name)) {
                    assertTrue(phase.supportsObservations());
                    phase.observation(new ReportObservation("Facts " + name, List.of(new ReportField("Source", name)),
                            List.of(new ReportObservation.Body("Complete text", "♠ −7 ".repeat(80) + "last-" + name, ReportObservation.Format.TEXT)), List.of()));
                    assertThrows(NullPointerException.class, () -> phase.observation(null));
                    phase.complete(AtfReportStatus.INFO);
                    assertThrows(IllegalStateException.class, () -> phase.observation(new ReportObservation("late", List.of(), List.of(), List.of())));
                }
            }
            scenario.complete(AtfReportStatus.INFO); scenario.close();
            var snapshot = session instanceof HtmlReportSession fallback ? fallback.semanticSnapshot() : ((ExtentReportSession) session).semanticSnapshot();
            assertEquals(0, snapshot.requestCount());
        }
        var profile = new ReportArtifactProfile(extent ? ReportArtifactProfile.Renderer.EXTENT : ReportArtifactProfile.Renderer.FALLBACK,
                List.of(new ReportArtifactProfile.Scenario("NeutralFacts", "phase ownership", AtfReportStatus.INFO,
                        List.of("SETUP", "TEST BODY", "TEARDOWN").stream().map(name -> new ReportArtifactProfile.Phase(name, AtfReportStatus.INFO,
                                List.of(new ReportArtifactProfile.Observation("Facts " + name, List.of("Source"), List.of("Complete text"))))).toList())));
        var inspection = ReportArtifactInspector.inspect(path, profile);
        assertTrue(inspection.isCompliant(), () -> inspection.violations().toString());
        var doc = Jsoup.parse(Files.readString(path));
        // The DOM oracle uses exact fact/body identity; scripts and templates cannot satisfy a phase.
        for (String name : List.of("SETUP", "TEST BODY", "TEARDOWN")) {
            var bodies = doc.select("pre").stream().filter(pre -> pre.wholeText().endsWith("last-" + name)).toList();
            assertEquals(1, bodies.size());
            assertTrue(bodies.getFirst().wholeText().length() > 240);
        }
        assertEquals(0, doc.select("table.helm-validation-table,table.assertions").size());
    }
}
