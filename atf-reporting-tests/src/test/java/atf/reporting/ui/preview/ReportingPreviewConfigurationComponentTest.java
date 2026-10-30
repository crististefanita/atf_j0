package atf.reporting.ui.preview;

import atf.reporting.configuration.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.console.AtfConsoleSink;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Resolved YAML settings and decoded System rows, independent of consumer metadata. */
@AtfComponentTest
class ReportingPreviewConfigurationComponentTest {
    @ParameterizedTest @CsvSource({"false,18", "true,18", "false,240", "true,240", "false,480", "true,480"})
    void systemShowsTheEffectiveBudgetWithoutAcceptingStaleCallerMetadata(boolean fallback, int limit,
                                                                         @TempDir Path output) throws Exception {
        Path yaml = output.resolve("atf.yaml");
        Files.writeString(yaml, limit == 240 ? "reporting: {}\n"
                : "reporting:\n  assertionMaxDisplayCharacters: " + limit + "\n");
        var settings = AtfReportConfigurationLoader.load(yaml).reportingSettings();
        assertEquals(limit, settings.assertionMaxDisplayCharacters(), "Default or configured YAML budget");
        Path file = output.resolve("preview-" + fallback + "-" + limit + ".html");
        var session = fallback ? new HtmlReportSession(file, settings) : new ExtentReportSession(file, settings,
                new SensitiveDataRedactor(), AtfConsoleSink.noOp(), Map.of("Assertion preview", "9999 characters"));
        try (session) {
            var scenario = session.startScenario("PreviewFixture", "configured preview", List.of("component"));
            try (var phase = scenario.startPhase("TEST BODY")) {
                try (var step = phase.startAssertionStep("Component assertions")) {
                    String prefix = "x".repeat(600);
                    step.assertions(List.of(new AtfAssertion("Full values differ", prefix + "expected", prefix + "actual", false)));
                    step.complete(AtfReportStatus.FAIL);
                }
                phase.complete(AtfReportStatus.FAIL);
            }
            scenario.complete(AtfReportStatus.FAIL);
            if (session instanceof ExtentReportSession extent) extent.updateEnvironment(ReportEnvironmentSnapshot.builder()
                    .put(ReportEnvironmentField.ASSERTION_PREVIEW, "1234 characters").build());
        }
        var rows = decodedRows(Files.readString(file));
        var systemRows = rows.stream().filter(row -> row.size() == 2 && row.getFirst().equals("Assertion preview")).toList();
        assertEquals(List.of(List.of("Assertion preview", limit + " characters")), systemRows,
                "Exactly one effective System setting, including after a caller update");
        var cells = rows.stream().flatMap(Collection::stream).toList();
        assertTrue(cells.contains("x".repeat(limit - 1) + "…"), "Entire preview including ellipsis follows the budget");
        assertFalse(cells.contains("x".repeat(600) + "actual"), "No unbounded value dump");
    }

    @ParameterizedTest @ValueSource(ints = {0, -1})
    void aNonPositiveAssertionBudgetIsRejectedBeforePublication(int limit, @TempDir Path output) throws Exception {
        Path yaml = output.resolve("invalid.yaml");
        Files.writeString(yaml, "reporting:\n  assertionMaxDisplayCharacters: " + limit + "\n");
        assertThrows(IllegalStateException.class, () -> AtfReportConfigurationLoader.load(yaml));
    }

    private static List<List<String>> decodedRows(String html) throws Exception {
        var rows = new ArrayList<List<String>>();
        var stack = new ArrayDeque<List<String>>();
        var cells = new ArrayDeque<StringBuilder>();
        new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
            @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int position) {
                if (tag == HTML.Tag.TR) stack.push(new ArrayList<>());
                if (tag == HTML.Tag.TD || tag == HTML.Tag.TH) cells.push(new StringBuilder());
            }
            @Override public void handleText(char[] value, int position) { if (!cells.isEmpty()) cells.peek().append(value); }
            @Override public void handleEndTag(HTML.Tag tag, int position) {
                if ((tag == HTML.Tag.TD || tag == HTML.Tag.TH) && !cells.isEmpty()) {
                    String value = cells.pop().toString().trim();
                    if (!stack.isEmpty()) stack.peek().add(value);
                }
                if (tag == HTML.Tag.TR && !stack.isEmpty()) rows.add(List.copyOf(stack.pop()));
            }
        }, true);
        return rows;
    }
}
