package atf.reporting.api.auth;

import atf.reporting.api.ApiEvidenceField;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.console.AtfConsoleSink;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiAuthenticationSchemaContractTest {
    @TempDir Path directory;

    @Test
    void dottedSpacedAndLegacyAliasesShareTheCanonicalVocabulary() {
        for (String key : List.of("auth.type", "Auth type")) {
            assertEquals("AUTH TYPE", EvidenceTableFormatter.displayName(key), key);
        }
        for (String key : List.of("auth.profile", "Auth profile")) {
            assertEquals("AUTH PROFILE", EvidenceTableFormatter.displayName(key), key);
        }
        assertEquals("AUTHENTICATION", EvidenceTableFormatter.displayName("auth.mode"),
                "the operational mode is not the request authentication mechanism");
        assertEquals("API 1 · AUTH TYPE",
                EvidenceTableFormatter.displayName("request.api.1.auth.type"));
        assertEquals("API 2 · AUTH PROFILE",
                EvidenceTableFormatter.displayName("response.api.2.auth.profile"));
    }

    @ParameterizedTest
    @CsvSource({
            "Bearer schema-secret, bearer, test-user",
            "Basic schema-secret, basic, test-user",
            "Digest schema-secret, other/unknown, test-user",
            "none, none, none"
    })
    void standaloneFallbackAndUiApiKeepProfileAndMechanismDistinct(
            String authorization, String mechanism, String profile) throws Exception {
        var request = new ReportRequest("GET", "/private", Map.of(),
                authorization.equals("none") ? Map.of() : Map.of("Authorization", authorization),
                profile, "");
        var response = new ReportResponse(200, "{}", Map.of("Content-Type", "application/json"),
                Duration.ofMillis(1), "https://api.example.test/private");
        var evidence = new ReportExecutionEvidence(response, 1, Duration.ofMillis(1),
                List.of(), List.of(), request, ReportTransportProvenance.ACCEPTANCE_MOCK);
        Path extent = directory.resolve("standalone.html");
        Path fallback = directory.resolve("fallback.html");
        for (AtfReportPorts.Session session : List.of(
                new ExtentReportSession(extent), new HtmlReportSession(fallback))) {
            try (session) {
                var scenario = session.startScenario("AuthContract", "auth schema", List.of("component"));
                var phase = scenario.startPhase("TEST BODY");
                var node = phase.startRequest("GET /private", "GET", "/private");
                node.detail(ApiEvidenceField.AUTH_PROFILE.label(), request.authProfile());
                node.detail(ApiEvidenceField.AUTH_TYPE.label(), request.authMethod().wireValue());
                node.execution(evidence);
                node.complete(AtfReportStatus.PASS);
                node.close();
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
        }
        Path ui;
        try (var session = UiExtentReportSession.openConfigured(directory, "ui-api.html", AtfConsoleSink.noOp())) {
            ui = session.reportPath();
            var scenario = session.startScenario("UiAuthContract", "auth", "account ready", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            phase.step(GIVEN, "account is ready", "/account", null,
                    List.of(new AtfAssertion("Account", "ready", "ready", true)), List.of(),
                    observedExchanges(List.of(evidence)), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        for (Path report : List.of(extent, fallback, ui)) {
            String html = Files.readString(report);
            assertEquals(List.of(profile), values(html, "AUTH PROFILE"), report.toString());
            assertEquals(List.of(mechanism), values(html, "AUTH TYPE"), report.toString());
            assertEquals(List.of(), values(html, "AUTHENTICATION"), report.toString());
            assertTrue(html.indexOf("AUTH PROFILE</td>") < html.indexOf("AUTH TYPE</td>"),
                    "profile and mechanism must retain the same field order in every projection");
            assertFalse(html.contains("schema-secret"), "credentials must not reach any report");
        }
    }

    private static List<String> values(String html, String label) {
        return Pattern.compile("<td[^>]*>" + Pattern.quote(label) + "</td>\\s*<td[^>]*>([^<]*)</td>")
                .matcher(html).results().map(match -> match.group(1)).toList();
    }
}
