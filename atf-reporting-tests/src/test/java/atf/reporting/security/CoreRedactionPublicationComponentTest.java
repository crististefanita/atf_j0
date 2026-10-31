package atf.reporting.security;

import atf.core.security.RedactionSettings;
import atf.core.security.SensitiveDataRedactor;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentest4j.AssertionFailedError;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Core sanitizes, Kit compares before preview, and each real renderer preserves the failed verdict. */
@AtfComponentTest
class CoreRedactionPublicationComponentTest {
    @ParameterizedTest
    @CsvSource({"false,$0", "true,$0", "false,$MASK\\", "true,$MASK\\"})
    void literalMasksSurviveTheComparisonAndPublicationBoundary(boolean fallback, String mask,
            @TempDir Path directory) throws Exception {
        var settings = RedactionSettings.defaults();
        var redactor = new SensitiveDataRedactor(new RedactionSettings(
                true, mask, false, false, settings.sensitiveFields()));
        var recorded = new RecordedAssertions(new AssertionValueDisplay(240, redactor::redactField));
        String expected = "Authorization: Bearer CORE_EXPECTED_CANARY; " + "x".repeat(300) + "A";
        String actual = "Authorization: Bearer CORE_ACTUAL_CANARY; " + "x".repeat(300) + "B";
        assertThrows(AssertionFailedError.class, () -> recorded.equals("Credential audit", expected, actual));
        var check = recorded.checks().getFirst();
        String safePrefix = "Authorization: Bearer " + mask + "; ";
        String independentPreview = safePrefix + "x".repeat(239 - safePrefix.length()) + "…";
        assertEquals(independentPreview, check.expected());
        assertEquals(independentPreview, check.actual());
        assertEquals(240, check.actual().length());
        assertFalse(check.passed(), "Identical safe previews cannot convert the original comparison to PASS");
        assertTrue(expected.contains("CORE_EXPECTED_CANARY"), "Original comparison inputs remain intact");
        Path output = directory.resolve(fallback ? "core-literal-fallback.html" : "core-literal-extent.html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(output, ReportingSettings.defaults())
                : new ExtentReportSession(output, ReportingSettings.defaults())) {
            try (var scenario = session.startScenario("CorePublication", "literal mask and bounded failure", List.of("component"));
                 var phase = scenario.startPhase("TEST BODY");
                 var step = phase.startAssertionStep("ASSERT audited comparison")) {
                step.assertions(List.of(new AtfAssertion(check.name(), check.expected(), check.actual(), check.passed())));
                step.complete(AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
            var snapshot = session instanceof HtmlReportSession html ? html.semanticSnapshot()
                    : ((ExtentReportSession) session).semanticSnapshot();
            assertEquals(1, snapshot.scenarioCount());
            assertEquals(0, snapshot.requestCount());
            assertEquals(AtfReportStatus.FAIL, snapshot.classes().getFirst().scenarios().getFirst().status());
        }
        String html = Files.readString(output);
        assertFalse(html.contains("CORE_EXPECTED_CANARY"));
        assertFalse(html.contains("CORE_ACTUAL_CANARY"));
        assertTrue(html.contains("Credential audit"));
        System.out.println("CORE_PUBLICATION_ARTIFACT=" + output);
    }
}
