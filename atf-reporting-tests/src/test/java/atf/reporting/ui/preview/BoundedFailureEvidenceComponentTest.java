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
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.text.MutableAttributeSet;
import java.io.StringReader;
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
            assertFalse(html.contains(longValue), "no hidden raw-value dump");
            assertFalse(html.contains("y".repeat(1358)), "Fails must use the same bounded projection");
            assertTrue(html.contains("REDACTED"));
            var values = cells(html);
            assertTrue(values.contains("x".repeat(239) + "…"));
            assertTrue(values.contains("y".repeat(239) + "…"));
            assertTrue(values.stream().filter(v -> v.startsWith("x")).allMatch(v -> v.length() <= 240));
            assertTrue(html.contains("BoundedFixture"));
        }
    }

    private static List<String> cells(String html) throws Exception {
        var values = new ArrayList<String>(); var stack = new ArrayDeque<StringBuilder>();
        new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
            @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int position) {
                if (tag == HTML.Tag.TD) stack.push(new StringBuilder());
            }
            @Override public void handleText(char[] text, int position) { if (!stack.isEmpty()) stack.peek().append(text); }
            @Override public void handleEndTag(HTML.Tag tag, int position) {
                if (tag == HTML.Tag.TD && !stack.isEmpty()) values.add(stack.pop().toString());
            }
        }, true);
        return values;
    }
}
