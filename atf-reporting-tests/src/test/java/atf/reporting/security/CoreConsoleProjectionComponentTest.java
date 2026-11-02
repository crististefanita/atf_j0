package atf.reporting.security;

import atf.core.security.SensitiveDataRedactor;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The source/JAR harness keeps the same credential policy across Core CODE and published JSON. */
@AtfComponentTest
class CoreConsoleProjectionComponentTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void completePublicJsonStringsUseTheSameKnownCredentialPolicy(boolean fallback, @TempDir Path directory) throws Exception {
        String canary = UUID.randomUUID().toString();
        String input = new ObjectMapper().writeValueAsString(Map.of("message", "Authorization: Bearer " + canary,
                "neighbor", "Independent public value", "amount", new java.math.BigDecimal("9007199254740993.01")));
        String console = new SensitiveDataRedactor().redactJson(input);
        var consoleJson = new ObjectMapper().readTree(console);
        assertFalse(console.contains(canary), "Core projection must remove the synthetic credential");
        assertTrue(console.contains("9007199254740993.01"));
        assertEquals("Independent public value", consoleJson.path("neighbor").asText());
        Path output = directory.resolve(fallback ? "public-strings-fallback.html" : "public-strings-extent.html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(output, new ReportingSettings(240, 0))
                : new ExtentReportSession(output, new ReportingSettings(240, 0));
             var scenario = session.startScenario("CoreProjection", "decoded public string", List.of("component"));
             var phase = scenario.startPhase("TEST BODY");
             var request = phase.startRequest("Captured JSON", "GET", "/fixture")) {
            request.json("Complete JSON", input);
            request.complete(AtfReportStatus.PASS); phase.complete(AtfReportStatus.PASS); scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(output);
        assertFalse(html.contains(canary), "Complete artifact must remove the synthetic credential");
        assertTrue(html.contains("9007199254740993.01"));
        assertTrue(html.contains("Independent public value"));
        assertTrue(input.contains(canary), "Comparison input remains immutable");
        System.out.println("CORE_STRING_ARTIFACT=" + output);
    }
}
