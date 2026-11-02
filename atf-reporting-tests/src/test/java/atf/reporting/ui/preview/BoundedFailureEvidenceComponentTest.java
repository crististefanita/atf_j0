package atf.reporting.ui.preview;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Extent/fallback publication; the oracle checks decoded values rather than markup length. */
@AtfComponentTest
class BoundedFailureEvidenceComponentTest {
    @Test void bothRenderersBoundRawAssertionsAndRedactCompleteActionValues() throws Exception {
        for (boolean extent : List.of(true, false)) {
            var file = TestReportPaths.report("bounded-failure-" + extent + "-" + UUID.randomUUID() + ".html");
            String longValue = "x".repeat(1358);
            String secret = "{\"password\":\"BOUNDED_EVIDENCE_CANARY_" + "q".repeat(350) + "\"}";
            try (AtfReportPorts.Session session = extent ? new ExtentReportSession(file, new ReportingSettings(240))
                    : new HtmlReportSession(file, new ReportingSettings(240))) {
                var scenario = session.startScenario("BoundedFixture", "long values", List.of("e2e"));
                var phase = scenario.startPhase("TEST BODY");
                try (var step = phase.startAssertionStep("Raw assertion")) {
                    step.assertions(List.of(new AtfAssertion("Long comparison", longValue, "y".repeat(1358), false)));
                    step.complete(AtfReportStatus.FAIL);
                }
                var request = phase.startRequest("Controlled UI evidence", "", "");
                var evidence = request.startValidation("Failure evidence");
                evidence.uiActions(List.of(
                        new AtfUiActionRecord(1, "Fill", "#field", secret, secret, 1, AtfReportStatus.PASS),
                        new AtfUiActionRecord(2, "Click", "#submit", "result ready", longValue, 20, AtfReportStatus.FAIL)));
                evidence.assertions(List.of(new AtfAssertion("Failure cause", "no failure", "controlled failure", false)));
                evidence.complete(AtfReportStatus.FAIL); evidence.close();
                request.complete(AtfReportStatus.FAIL); request.close();
                phase.complete(AtfReportStatus.FAIL); scenario.complete(AtfReportStatus.FAIL);
            }
            String html = Files.readString(file);
            assertFalse(html.contains("BOUNDED_EVIDENCE_CANARY_"), "redact the whole JSON before applying its budget");
            assertFalse(html.contains("y".repeat(1358)), "Fails must use the same bounded projection");
            assertTrue(html.contains("REDACTED"));
            var doc = Jsoup.parse(html);
            var assertions = doc.select("table.helm-validation-table,table.assertions");
            var values = assertions.select("tbody > tr > td").eachText();
            assertTrue(values.contains("x".repeat(239) + "…"));
            assertTrue(values.contains("y".repeat(239) + "…"));
            assertTrue(values.stream().filter(v -> v.startsWith("x")).allMatch(v -> v.length() <= 240));
            assertFalse(assertions.outerHtml().contains(longValue), "Assertions keep the original bounded contract");
            var observed = doc.select("table.atf-ui-action-table tbody > tr").get(1).child(3);
            var complete = observed.selectFirst("details.atf-ui-value");
            assertNotNull(complete, "Captured action value is an auditable, owned disclosure");
            assertFalse(complete.hasAttr("open"));
            assertEquals("x".repeat(239) + "…", complete.selectFirst("summary .se-title").text());
            assertEquals(longValue, complete.selectFirst("pre").wholeText());
            assertEquals(1, doc.select(".atf-ui-action-result-fail").size());
            complete.selectFirst("pre").remove();
            assertFalse(doc.outerHtml().contains(longValue), "Only the deliberate action disclosure owns the complete scalar");
            assertTrue(html.contains("BoundedFixture"));
        }
    }
}
