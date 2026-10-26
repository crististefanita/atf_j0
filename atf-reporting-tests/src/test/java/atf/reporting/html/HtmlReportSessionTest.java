package atf.reporting.html;
import atf.testkit.AtfUnitTest;

import atf.reporting.support.TestReportPaths;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.support.ApiReportTestUrls;

import atf.reporting.configuration.ReportingSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class HtmlReportSessionTest {
    @Test
    void rendersApprovedStructureWithValidationAsRequestSibling() {
        var session = new HtmlReportSession(TestReportPaths.report("report.html"));
        var scenario = session.startScenario("UsersTests", "get user", List.of("api", "contract"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("item contract", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        request.detail("response.status", "200");
        request.json("RESPONSE JSON", "{\"id\":1}");
        var validation = request.startValidation("Validate user");
        validation.assertions(List.of(
                new AtfAssertion("HTTP status", "200", "200", true),
                new AtfAssertion("schema", "matched", "mismatch", false)));
        validation.complete(AtfReportStatus.FAIL);
        request.complete(AtfReportStatus.FAIL);
        phase.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);

        var html = session.render();

        assertTrue(html.indexOf("GET " + ApiReportTestUrls.GENERIC_USER_ONE) < html.indexOf("Validate user"));
        int requestIndex = html.indexOf("class=\"request\"");
        int validationIndex = html.indexOf("class=\"validation\" data-atf-node-role=\"validation\"");
        assertTrue(validationIndex > requestIndex
                        && divDepthAt(html, validationIndex) > divDepthAt(html, requestIndex),
                "validation must be nested inside the Gherkin request node");
        assertTrue(html.contains("class=\"assertions\""));
        assertTrue(html.contains("class=\"atf-validation-table-wrap\""));
        assertTrue(html.indexOf("class=\"atf-validation-table-wrap\"")
                        < html.indexOf("class=\"assertions\""));
        assertTrue(html.contains("HTTP status"));
        assertTrue(html.contains("schema"));
        assertTrue(html.contains("class=\"json-code\""));
        assertTrue(html.contains("href=\"#test-id=1\""));
        assertEquals(1, count(html, "<nav class=\"tabs\">") );
        assertTrue(html.indexOf(">Category<") < html.indexOf(">Fails<"));
        assertTrue(html.indexOf(">Fails<") < html.indexOf(">Overview<"));
        assertTrue(html.contains("data-tab=\"category\""));
        assertTrue(html.contains("id=\"category\""));
        assertTrue(html.contains("--atf-dark:#101723"));
        assertTrue(html.contains("font-family:'Segoe UI','Inter','Helvetica Neue',Arial,sans-serif"));
        assertTrue(html.contains("font-size:12.6px;line-height:16.884px"));
        assertTrue(html.contains("font:12.5px/18.75px ui-monospace,Consolas,monospace"));
        assertFalse(html.contains("--bg:#0e1624"));
        assertFalse(html.contains("font:14px Arial,sans-serif"));
        assertTrue(html.contains("activate('overview')"));
        assertFalse(html.contains("extent/"));
        assertTrue(html.contains("<link rel=\"icon\" href=\"data:,\""),
                "the offline fallback must not request the host's implicit favicon");
    }

    @Test
    void flushCreatesAnAutonomousArtifact() throws Exception {
        Path output = Files.createTempDirectory("atf-report-").resolve("report.html");
        try (var session = new HtmlReportSession(output)) {
            var scenario = session.startScenario("Tests", "empty", List.of());
            scenario.complete(AtfReportStatus.PASS);
        }
        assertTrue(Files.exists(output));
        assertTrue(Files.readString(output).contains("<!doctype html>"));
    }

    @Test
    void fallbackRendererKeepsPhaseShellsAndEvidenceRolesExplicit() {
        var session = new HtmlReportSession(TestReportPaths.report("fallback-contract.html"));
        var scenario = session.startScenario("UsersApiTests", "user contract", List.of("api", "contract"));
        var setup = scenario.startPhase("SETUP");
        setup.startRequest("seed", "GET", ApiReportTestUrls.GENERIC_USERS).complete(AtfReportStatus.PASS);
        setup.complete(AtfReportStatus.PASS);

        var body = scenario.startPhase("TEST BODY");
        var request = body.startRequest("item contract", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        request.detail("RESPONSE DURATION (ms)", "12");
        request.detail("URL", ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_USER_ONE));
        request.detail("ROUTE TEMPLATE", ApiReportTestUrls.GENERIC_USERS + "/{id}");
        request.detail("HTTP STATUS", "200");
        request.execution(new ApiExecutionResult(
                new ApiResponse(200, "OK", Map.of(), Duration.ofMillis(12),
                        ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_USER_ONE)),
                1, Duration.ofMillis(12), List.of(), List.of()));
        request.json("REQUEST JSON", "{\"id\":1}");
        request.json("RESPONSE JSON", "{\"id\":1}");
        var validation = request.startValidation("Validate: user contract");
        validation.assertions(List.of(new AtfAssertion("HTTP status", "200", "200", true)));
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
        body.complete(AtfReportStatus.PASS);

        var teardown = scenario.startPhase("TEARDOWN");
        teardown.startRequest("cleanup", "DELETE", ApiReportTestUrls.GENERIC_USER_ONE).complete(AtfReportStatus.PASS);
        teardown.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        var html = session.render();

        assertAll(
                () -> assertEquals(3, count(html, "class=\"phase phase-pass\"")),
                () -> assertTrue(html.contains("data-atf-node-role=\"phase\" data-atf-phase-name=\"test-body\"")),
                () -> assertTrue(html.contains("data-atf-node-role=\"request\"")),
                () -> assertTrue(html.contains("data-atf-node-role=\"validation\"")),
                () -> assertTrue(html.indexOf("class=\"request\"") < html.indexOf("class=\"validation\"")),
                () -> assertEquals(1, count(html, "class=\"assertions\"")),
                () -> assertTrue(html.contains("class=\"json-code\"")),
                () -> assertTrue(html.contains("class=\"atf-disclosure-glyph\"")),
                () -> assertTrue(html.contains("Request details · 2 fields")),
                () -> assertTrue(html.contains("Response details · 1 field")),
                () -> assertTrue(html.contains("Execution evidence · 1 attempt")),
                () -> assertTrue(html.contains("RESPONSE DURATION")),
                () -> assertTrue(html.contains("execution-evidence")),
                () -> assertTrue(html.contains("request-details-content")),
                () -> assertTrue(html.contains("response-details-content")),
                () -> assertTrue(html.contains("request-json-body")),
                () -> assertTrue(html.contains("response-json-body")),
                () -> assertTrue(html.contains("helm-api-classification-label\">TECHNICAL TRACE</span>")),
                () -> assertFalse(html.contains("[+] expand")),
                () -> assertTrue(html.contains(
                        "<div class=\"atf-trace-table-wrap\" tabindex=\"0\"><table class=\"helm-evidence-table helm-trace-table\">")),
                () -> assertTrue(html.indexOf("</table></div>", html.indexOf(
                                "class=\"atf-trace-table-wrap\""))
                                < html.indexOf("class=\"response-json-body\""),
                        "the fallback trace viewport must end before the separate response JSON pane"),
                 () -> assertTrue(html.contains("class=\"helm-evidence-table helm-request-table\"")),
                () -> assertTrue(html.contains("class=\"phase phase-pass\"")),
                () -> assertTrue(html.contains("class=\"tag\">api</span>")),
                () -> assertFalse(html.contains("Timestamp")),
                () -> assertFalse(html.contains("<small>api</small>")));
    }

    @Test
    void fallbackJsonBodyIsScrollableAndUsesTheConfiguredDisplayLimit() {
        var session = new HtmlReportSession(TestReportPaths.report("bounded-json.html"),
                new ReportingSettings(240, 18));
        var scenario = session.startScenario("BodyTests", "bounded response", List.of("api"));
        var body = scenario.startPhase("TEST BODY");
        var request = body.startRequest("large response", "GET", ApiReportTestUrls.GENERIC_USERS);
        request.json("RESPONSE JSON", "{\"payload\":\"012345678901234567890123456789\"}");
        request.complete(AtfReportStatus.PASS);
        body.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        var html = session.render();

        assertAll(
                () -> assertTrue(html.contains("class=\"response-json-body\"")),
                () -> assertTrue(html.contains("class=\"json-code\"")),
                () -> assertTrue(html.contains("max-height:360px;overflow:auto")),
                () -> assertTrue(html.contains("… [truncated; showing first 18 of ")),
                () -> assertFalse(html.contains("012345678901234567890123456789\"}")));
    }

    @Test
    void fallbackRedactsFailureCauseWhenPublishedThroughACompletedPhase() {
        var session = new HtmlReportSession(TestReportPaths.report("fallback-failure-redaction.html"));
        var scenario = session.startScenario("FailureTests", "redacted failure", List.of("api"));
        var phase = scenario.startPhase("TEST BODY");
        phase.detail("Failure cause", "ApiValidationException: token=raw-failure-token; mode=full");
        phase.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);

        var html = session.render();

        assertAll(
                () -> assertTrue(html.contains("Failure cause")),
                () -> assertTrue(html.contains("token=***REDACTED***")),
                () -> assertTrue(html.contains("mode=full")),
                () -> assertFalse(html.contains("raw-failure-token")));
    }

    private static int count(String value, String token) {
        int count = 0, index = 0;
        while ((index = value.indexOf(token, index)) >= 0) { count++; index += token.length(); }
        return count;
    }

    private static int divDepthAt(String html, int markerIndex) {
        String prefix = html.substring(0, markerIndex);
        int depth = 0;
        for (int index = 0; index < prefix.length(); index++) {
            if (prefix.startsWith("<div", index)) depth++;
            if (prefix.startsWith("</div>", index)) depth--;
        }
        return depth;
    }
}
